// Jenkinsfile for Monorepo with multiple microservices (updated for Polyglot support)

pipeline {
    agent any

    stages {
        stage('Detect Changed Services') {
            steps {
                script {
                    echo "Checking for changes..."

                    def changedFiles = bat(returnStdout: true, script: 'git diff --name-only HEAD~1 HEAD').trim().split('\r\n')
                    echo "Changed files reported by Git: ${changedFiles}"

                    // ======================================================================
                    // [수정됨] 서비스 탐지 기준 변경: 'gradlew.bat' 대신 'Dockerfile'이 있는 모든 폴더를 서비스로 간주합니다.
                    // 이렇게 하면 Java, Python 등 모든 유형의 마이크로서비스를 인식할 수 있습니다.
                    // ======================================================================
                    def servicePaths = bat(returnStdout: true, script: "dir /s /b Dockerfile").trim().split('\r\n').collect { it.replace('\\Dockerfile', '') }
                    def workspacePath = env.WORKSPACE
                    def relativeServicePaths = servicePaths.collect { it.replace(workspacePath, '').replaceAll('^\\\\', '') }
                    echo "Found relative service paths (based on Dockerfile): ${relativeServicePaths}"

                    def changedServices = []

                    for (String file in changedFiles) {
                        def windowsStyleFile = file.replace('/', '\\')
                        for (String servicePath in relativeServicePaths) {
                            if (windowsStyleFile.startsWith(servicePath + '\\') && !changedServices.contains(servicePath)) {
                                changedServices.add(servicePath)
                                echo "SUCCESS: Detected change in service -> ${servicePath}"
                            }
                        }
                    }

                    if (changedServices.isEmpty()) {
                        echo "No changes detected in any service directory. Skipping build."
                        currentBuild.result = 'NOT_BUILT'
                        return
                    }

                    env.CHANGED_SERVICES = changedServices.join(',')
                }
            }
        }

        stage('Build and Push Changed Services') {
            when {
                expression { env.CHANGED_SERVICES != null && env.CHANGED_SERVICES != '' }
            }
            steps {
                script {
                    def changedServicesList = env.CHANGED_SERVICES.split(',')
                    def parallelStages = [:]

                    for (String servicePath in changedServicesList) {
                        parallelStages["Build & Push ${servicePath}"] = {
                            ws(servicePath) {
                                echo "--- Starting build & push for ${servicePath} ---"
                                try {
                                    // ======================================================================
                                    // [추가됨] 프로젝트 유형(Java/Python)을 감지하고 그에 맞는 빌드를 실행하는 로직
                                    // ======================================================================
                                    stage("Build Application: ${servicePath}") {
                                        if (fileExists('gradlew.bat')) {
                                            echo "Detected Java/Gradle project. Running Gradle build..."
                                            bat 'gradlew.bat clean bootJar'
                                        } else if (fileExists('requirements.txt')) {
                                            echo "Detected Python project. Skipping build step (handled in Dockerfile)."
                                            // Python 프로젝트는 보통 Dockerfile 안에서 'pip install'을 하므로 별도의 빌드 단계는 생략합니다.
                                        } else {
                                            error "Unknown project type in ${servicePath}. No gradlew.bat or requirements.txt found."
                                        }
                                    }

                                    def serviceName = new File(servicePath).name
                                    def imageName = "berrymas/${serviceName}:${env.BUILD_NUMBER}"

                                    stage("Docker Build: ${servicePath}") {
                                        bat "docker build -t ${imageName} ."
                                    }
                                    stage("Push Docker Image: ${servicePath}") {
                                        docker.withRegistry('https://index.docker.io/v1/', 'dockerhub-credentials') {
                                            bat "docker push ${imageName}"
                                        }
                                    }
                                } catch (e) {
                                    error("Build or push failed for ${servicePath}")
                                }
                            }
                        }
                    }
                    parallel parallelStages
                }
            }
        }
    }

    post {
        always {
            echo 'Pipeline finished.'
            cleanWs()
        }
    }
}

