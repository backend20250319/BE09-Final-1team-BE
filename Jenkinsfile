// Jenkinsfile for Monorepo with multiple microservices (updated for Windows path matching)

pipeline {
    agent any

    stages {
        stage('Detect Changed Services') {
            steps {
                script {
                    echo "Checking for changes..."

                    // 1. 변경된 파일 목록 가져오기 (결과는 상대 경로)
                    def changedFiles = bat(returnStdout: true, script: 'git diff --name-only HEAD~1 HEAD').trim().split('\r\n')
                    echo "Changed files reported by Git: ${changedFiles}"

                    // ======================================================================
                    // [수정됨] 서비스 경로를 찾는 방식을 '상대 경로' 기준으로 변경합니다.
                    // ======================================================================

                    // 2. 먼저 모든 'gradlew.bat' 파일의 전체 경로를 찾습니다.
                    def fullServiceGradlePaths = bat(returnStdout: true, script: 'dir /s /b gradlew.bat').trim()

                    if (!fullServiceGradlePaths) {
                        error("FATAL: No 'gradlew.bat' files found in the workspace. Cannot determine service directories.")
                    }

                    // 3. Jenkins의 현재 작업 공간(Workspace) 경로를 가져옵니다.
                    def workspacePath = env.WORKSPACE
                    echo "Current workspace path is: ${workspacePath}"

                    // 4. 전체 경로를 작업 공간 기준의 '상대 경로'로 변환합니다.
                    def servicePaths = fullServiceGradlePaths.split('\r\n').collect { fullPath ->
                        def serviceDir = fullPath.replace('\\gradlew.bat', '')
                        // 작업 공간 경로 부분을 제거하여 상대 경로로 만듭니다.
                        def relativePath = serviceDir.replace(workspacePath, '').replaceAll('^\\\\', '')
                        return relativePath
                    }
                    echo "Found relative service paths: ${servicePaths}"
                    // ======================================================================

                    def changedServices = []

                    // 5. '상대 경로'끼리 비교하여 변경된 서비스를 찾습니다.
                    for (String file in changedFiles) {
                        def windowsStyleFile = file.replace('/', '\\') // Git 경로를 Windows 형식으로 변경
                        for (String servicePath in servicePaths) {
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

        // Build and Push 단계는 이전과 동일합니다.
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
                                    def serviceName = new File(servicePath).name
                                    def imageName = "apocalcal/${serviceName}:${env.BUILD_NUMBER}"

                                    stage("Gradle Build: ${servicePath}") {
                                        bat 'gradlew.bat clean bootJar'
                                    }
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

