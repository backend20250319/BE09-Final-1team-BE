// Jenkinsfile for Monorepo with multiple microservices (updated for nested structure)

pipeline {
    agent any // 빌드를 실행할 Jenkins 에이전트를 지정합니다.

    stages {
        // 1단계: 어떤 서비스의 코드가 변경되었는지 감지하는 단계
        stage('Detect Changed Services') {
            steps {
                script {
                    echo "Checking for changes..."
                    // 가장 최근의 push에서 어떤 파일이 변경되었는지 찾아냅니다.
                    def changedFiles = sh(returnStdout: true, script: 'git diff --name-only HEAD~1 HEAD').trim().split('\n')

                    // ======================================================================
                    // 'gradlew' 파일의 위치를 기반으로 모든 서비스 폴더의 경로를 찾습니다.
                    // 이렇게 하면 'config/config-server', 'services/discovery-service' 등을 모두 찾을 수 있습니다.
                    def servicePaths = sh(returnStdout: true, script: "find . -name 'gradlew' -printf '%h\\n' | sed 's|^./||'").trim().split('\\n')
                    // ======================================================================

                    def changedServices = []

                    // 변경된 파일이 어떤 서비스 폴더 경로에 속하는지 확인합니다.
                    for (String file in changedFiles) {
                        for (String servicePath in servicePaths) {
                            if (file.startsWith(servicePath + '/') && !changedServices.contains(servicePath)) {
                                changedServices.add(servicePath)
                                echo "Detected change in service: ${servicePath}"
                            }
                        }
                    }

                    // 변경된 서비스가 없으면, 파이프라인을 더 이상 진행하지 않고 중단합니다.
                    if (changedServices.isEmpty()) {
                        echo "No changes detected in any service directory. Skipping build."
                        currentBuild.result = 'NOT_BUILT' // 빌드 기록에 '실행 안 됨'으로 표시
                        return
                    }

                    // 변경된 서비스 목록(전체 경로)을 다음 단계에서 사용할 수 있도록 환경 변수에 저장합니다.
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

                    // 변경된 각 서비스에 대해 빌드 및 푸시 작업을 동적으로 생성합니다.
                    for (String servicePath in changedServicesList) {
                        parallelStages["Build & Push ${servicePath}"] = {
                            dir(servicePath) {
                                echo "--- Starting build & push for ${servicePath} ---"
                                try {
                                    // [수정됨] imageName 변수를 try 블록의 상단으로 이동시켜 모든 stage에서 접근 가능하게 합니다.
                                    def serviceName = new File(servicePath).name
                                    // ⚠️ 중요: 'your-dockerhub-username'을 반드시 수정해주세요!
                                    def imageName = "berrymas/${serviceName}:${env.BUILD_NUMBER}"

                                    stage("Gradle Build: ${servicePath}") {
                                        sh 'chmod +x ./gradlew'
                                        sh './gradlew clean bootJar'
                                    }
                                    stage("Docker Build: ${servicePath}") {
                                        sh "docker build -t ${imageName} ."
                                        echo "Successfully built Docker image: ${imageName}"
                                    }
                                    // [추가됨] Docker 이미지를 Docker Hub로 푸시하는 단계
                                    stage("Push Docker Image: ${servicePath}") {
                                        // 'dockerhub-credentials'는 Jenkins에 저장된 Credential의 ID입니다.
                                        docker.withRegistry('https://index.docker.io/v1/', 'dockerhub-credentials') {
                                            sh "docker push ${imageName}"
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

