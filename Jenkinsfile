// Jenkinsfile for Monorepo with multiple microservices (updated for Windows environment)

pipeline {
    agent any // 빌드를 실행할 Jenkins 에이전트를 지정합니다.

    stages {
        // 1단계: 어떤 서비스의 코드가 변경되었는지 감지하는 단계
        stage('Detect Changed Services') {
            steps {
                script {
                    echo "Checking for changes..."
                    // [수정됨] sh -> bat. Git 명령어는 Windows에서도 동일하게 작동합니다.
                    def changedFiles = bat(returnStdout: true, script: 'git diff --name-only HEAD~1 HEAD').trim().split('\r\n')

                    // ======================================================================
                    // [수정됨] 리눅스 명령어 'find', 'sed'를 윈도우 명령어 'dir'로 변경합니다.
                    // Windows 환경에서 모든 'gradlew.bat' 파일의 경로를 찾습니다.
                    def servicePaths = bat(returnStdout: true, script: 'dir /s /b gradlew.bat').trim().split('\r\n').collect { it.replace('\\gradlew.bat', '') }
                    // ======================================================================

                    def changedServices = []

                    // 변경된 파일이 어떤 서비스 폴더 경로에 속하는지 확인합니다.
                    for (String file in changedFiles) {
                        for (String servicePath in servicePaths) {
                            // [수정됨] Windows 경로 구분자인 '\'를考慮하여 로직 수정
                            if (file.replace('/', '\\').startsWith(servicePath + '\\') && !changedServices.contains(servicePath)) {
                                changedServices.add(servicePath)
                                echo "Detected change in service: ${servicePath}"
                            }
                        }
                    }

                    // 변경된 서비스가 없으면, 파이프라인을 더 이상 진행하지 않고 중단합니다.
                    if (changedServices.isEmpty()) {
                        echo "No changes detected in any service directory. Skipping build."
                        currentBuild.result = 'NOT_BUILT'
                        return
                    }

                    env.CHANGED_SERVICES = changedServices.join(',')
                }
            }
        }

        // 2단계: 변경이 감지된 서비스들을 빌드하고 푸시하는 단계
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
                            // [수정됨] dir -> ws. Windows에서 폴더 경로를 지정할 때는 ws를 사용하는 것이 더 안정적입니다.
                            ws(servicePath) {
                                echo "--- Starting build & push for ${servicePath} ---"
                                try {
                                    def serviceName = new File(servicePath).name
                                    def imageName = "berrymas/${serviceName}:${env.BUILD_NUMBER}" // Docker Hub 사용자 이름은 그대로 유지

                                    stage("Gradle Build: ${servicePath}") {
                                        // [제거됨] chmod는 Windows에 필요 없는 명령어입니다.
                                        // [수정됨] sh -> bat, ./gradlew -> gradlew.bat
                                        bat 'gradlew.bat clean bootJar'
                                    }
                                    stage("Docker Build: ${servicePath}") {
                                        // [수정됨] sh -> bat
                                        bat "docker build -t ${imageName} ."
                                        echo "Successfully built Docker image: ${imageName}"
                                    }
                                    stage("Push Docker Image: ${servicePath}") {
                                        docker.withRegistry('https://index.docker.io/v1/', 'dockerhub-credentials') {
                                            // [수정됨] sh -> bat
                                            bat "docker push ${imageName}"
                                            echo "Successfully pushed Docker image: ${imageName}"
                                        }
                                    }
                                } catch (e) {
                                    echo "Failed to build or push service ${servicePath}"
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

