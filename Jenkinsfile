// 빌드 결과를 저장하기 위한 전역 변수를 선언합니다.
def buildResults = [succeeded: [], failed: []]

pipeline {
    agent any

    tools {
        jdk 'jdk17'
    }

    stages {
        stage('Detect Changed Services') {
            steps {
                script {
                    echo "Checking for changes..."

                    def changedFiles
                    if (env.GIT_PREVIOUS_COMMIT) {
                        echo "Comparing current commit (${env.GIT_COMMIT}) with previous build commit (${env.GIT_PREVIOUS_COMMIT})"
                        def commandOutput = bat(returnStdout: true, script: "git diff --name-only ${env.GIT_PREVIOUS_COMMIT} ${env.GIT_COMMIT}").trim()
                        changedFiles = commandOutput.split('\r\n').findAll { line -> !line.contains('>') && line.trim() != '' }
                    } else {
                        echo "This is the first build. All services will be built."
                        def commandOutput = bat(returnStdout: true, script: 'git ls-files').trim()
                        changedFiles = commandOutput.split('\r\n').findAll { line -> !line.contains('>') && line.trim() != '' }
                    }
                    echo "Cleaned changed files list: ${changedFiles}"

                    // [수정됨] dir 명령어 결과에서도 불필요한 프롬프트 라인을 필터링합니다.
                    def servicePathsOutput = bat(returnStdout: true, script: "dir /s /b Dockerfile").trim()
                    def servicePaths = servicePathsOutput.split('\r\n').findAll { line -> !line.contains('>') && line.trim() != '' }.collect { it.replace('\\Dockerfile', '') }

                    def workspacePath = env.WORKSPACE
                    def relativeServicePaths = servicePaths.collect { it.replace(workspacePath, '').replaceAll('^\\\\', '') }
                    echo "Found relative service paths (based on Dockerfile): ${relativeServicePaths}"

                    def changedServices = new HashSet<String>()

                    for (String file in changedFiles) {
                        def windowsStyleFile = file.replace('/', '\\')
                        for (String servicePath in relativeServicePaths) {
                            if (windowsStyleFile.startsWith(servicePath + '\\')) {
                                if (changedServices.add(servicePath)) {
                                     echo "SUCCESS: Detected change in service -> ${servicePath}"
                                }
                            }
                        }
                    }

                    // [디버깅 추가] 최종적으로 감지된 서비스 목록을 확인합니다.
                    echo "Final list of changed services to be built: ${changedServices.toList()}"

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
                    // [디버깅 추가] Build 단계가 받은 서비스 목록을 확인합니다.
                    echo "Build stage received the following services: ${env.CHANGED_SERVICES}"
                    def changedServicesList = env.CHANGED_SERVICES.split(',')
                    echo "Services list after splitting: ${changedServicesList}"

                    def parallelStages = [:]

                    for (String servicePath in changedServicesList) {
                        parallelStages["Build & Push ${servicePath}"] = {
                            dir(servicePath) {
                                echo "--- Starting build & push for ${servicePath} ---"
                                try {
                                    stage("Build Application: ${servicePath}") {
                                        if (fileExists('gradlew.bat')) {
                                            bat 'gradlew.bat clean bootJar'
                                        }
                                    }

                                    def serviceName = servicePath.replace('\\', '/').split('/').last()
                                    def imageName = "berrymas/${serviceName}:${env.BUILD_NUMBER}"

                                    stage("Docker Build & Push: ${servicePath}") {
                                        bat "docker build -t ${imageName} ."
                                        docker.withRegistry('https://index.docker.io/v1/', 'dockerhub-credentials') {
                                            bat "docker push ${imageName}"
                                        }
                                    }
                                    buildResults.succeeded.add(servicePath)
                                } catch (e) {
                                    buildResults.failed.add(servicePath)
                                    echo "ERROR during build or push for ${servicePath}: ${e.toString()}"
                                }
                            }
                        }
                    }
                    parallel parallelStages

                    if (!buildResults.failed.isEmpty()) {
                        error("One or more services failed to build: ${buildResults.failed.join(', ')}")
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                echo '--- Build Summary ---'
                if (!buildResults.succeeded.isEmpty()) {
                    echo "✅ Succeeded services: ${buildResults.succeeded.join(', ')}"
                }
                if (!buildResults.failed.isEmpty()) {
                    echo "❌ Failed services: ${buildResults.failed.join(', ')}"
                }
                if (currentBuild.result == 'NOT_BUILT') {
                    echo "- No services were built as no changes were detected."
                }
                echo '---------------------'
                cleanWs()
            }
        }
    }
}