// monorepo/Jenkinsfile

pipeline {
    agent any
    
    environment {
        // AWS 설정
        AWS_DEFAULT_REGION = 'ap-northeast-2'
        AWS_ACCOUNT_ID = '883467884806' // 민감정보가 아니므로 일반 문자열로 관리
        ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com"
        
        // Jenkins Credentials ID
        AWS_CREDENTIALS_ID = 'aws-credentials' // Jenkins에 등록된 AWS 자격증명 ID
        GIT_CREDENTIALS_ID = 'github-ssh-key' // Jenkins에 등록된 GitHub SSH 배포 키 ID

        // Kubernetes Manifests 리포지토리 정보
        MANIFEST_REPO_URL = 'git@github.com:backend20250319/BE09-Final-1team-BE.git' // SSH 주소 사용
        
        // EKS 설정
        EKS_CLUSTER_NAME = 'msa-cluster'
        EKS_NAMESPACE = 'msa-namespace'
        
        // Docker 이미지 태그
        IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.take(7)}"
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    echo "빌드 시작 (태그: ${IMAGE_TAG})"
                }
            }
        }
        
        stage('Build & Push Changed Services') {
            parallel {
                stage('Config Server') {
                    when { changeset "config/config-server/**" }
                    steps {
                        script {
                            def serviceName = 'config-server'
                            def servicePath = "config/${serviceName}"
                            def image = "${ECR_REGISTRY}/${serviceName}:${IMAGE_TAG}"
                            buildAndPush(serviceName, servicePath, image)
                        }
                    }
                }
                stage('Discovery Service') {
                    when { changeset "services/discovery-service/**" }
                    steps {
                        script {
                            def serviceName = 'discovery-service'
                            def servicePath = "services/${serviceName}"
                            def image = "${ECR_REGISTRY}/${serviceName}:${IMAGE_TAG}"
                            buildAndPush(serviceName, servicePath, image)
                        }
                    }
                }
                stage('Gateway Service') {
                    when { changeset "services/gateway-service/**" }
                    steps {
                        script {
                            def serviceName = 'gateway-service'
                            def servicePath = "services/${serviceName}"
                            def image = "${ECR_REGISTRY}/${serviceName}:${IMAGE_TAG}"
                            buildAndPush(serviceName, servicePath, image)
                        }
                    }
                }
                stage('User Service') {
                    when { changeset "services/user-service/**" }
                    steps {
                        script {
                            def serviceName = 'user-service'
                            def servicePath = "services/${serviceName}"
                            def image = "${ECR_REGISTRY}/${serviceName}:${IMAGE_TAG}"
                            buildAndPush(serviceName, servicePath, image)
                        }
                    }
                }
                stage('News Service') {
                    when { changeset "services/news-service/**" }
                    steps {
                        script {
                            def serviceName = 'news-service'
                            def servicePath = "services/${serviceName}"
                            def image = "${ECR_REGISTRY}/${serviceName}:${IMAGE_TAG}"
                            buildAndPush(serviceName, servicePath, image)
                        }
                    }
                }
                stage('Flask API') {
                    when { changeset "services/flaskapi/**" }
                    steps {
                        script {
                            def serviceName = 'flaskapi'
                            def servicePath = "services/${serviceName}"
                            def image = "${ECR_REGISTRY}/${serviceName}:${IMAGE_TAG}"
                            buildAndPush(serviceName, servicePath, image)
                        }
                    }
                }
                stage('Crawler Service') {
                    when { changeset "services/crawler-service/**" }
                    steps {
                        script {
                            def serviceName = 'crawler-service'
                            def servicePath = "services/${serviceName}"
                            def image = "${ECR_REGISTRY}/${serviceName}:${IMAGE_TAG}"
                            buildAndPush(serviceName, servicePath, image)
                        }
                    }
                }
                stage('Dedup Service') {
                    when { changeset "services/dedup-service/**" }
                    steps {
                        script {
                            def serviceName = 'dedup-service'
                            def servicePath = "services/${serviceName}"
                            def image = "${ECR_REGISTRY}/${serviceName}:${IMAGE_TAG}"
                            buildAndPush(serviceName, servicePath, image)
                        }
                    }
                }
                stage('Newsletter Service') {
                    when { changeset "services/newsletter-service/**" }
                    steps {
                        script {
                            def serviceName = 'newsletter-service'
                            def servicePath = "services/${serviceName}"
                            def image = "${ECR_REGISTRY}/${serviceName}:${IMAGE_TAG}"
                            buildAndPush(serviceName, servicePath, image)
                        }
                    }
                }
                stage('Tooltip Service') {
                    when { changeset "services/tooltip-service/**" }
                    steps {
                        script {
                            def serviceName = 'tooltip-service'
                            def servicePath = "services/${serviceName}"
                            def image = "${ECR_REGISTRY}/${serviceName}:${IMAGE_TAG}"
                            buildAndPush(serviceName, servicePath, image)
                        }
                    }
                }
            }
        }

        stage('Update Deployment Manifests') {
            steps {
                script {
                    // GitOps: 매니페스트 리포지토리에 변경 사항을 커밋하고 푸시
                    withCredentials([sshUserPrivateKey(credentialsId: GIT_CREDENTIALS_ID, keyFileVariable: 'GIT_KEY')]) {
                        sh """
                            # 1. 매니페스트 리포지토리 클론
                            git clone ${MANIFEST_REPO_URL} manifests-repo
                            cd manifests-repo

                            # 2. Git 사용자 설정
                            git config user.name "Jenkins CI"
                            git config user.email "jenkins@example.com"
                        """

                        // 변경된 서비스 목록을 기반으로 매니페스트 업데이트
                        def changedServices = findChangedServices()
                        if (!changedServices.isEmpty()) {
                            echo "Updating manifests for: ${changedServices}"
                            changedServices.each { serviceName ->
                                def image = "${ECR_REGISTRY}/${serviceName}:${IMAGE_TAG}"
                                echo "Updating ${serviceName} image to ${image}"
                                // yq 또는 sed를 사용하여 이미지 태그 업데이트 (yq가 더 안정적입니다)
                                sh "sed -i 's|image:.*|image: ${image}|g' ./${serviceName}/deployment.yml"
                            }
                            sh """
                                # 4. 변경사항 커밋 및 푸시
                                git add .
                                git commit -m "Update image tag to ${IMAGE_TAG} by Jenkins Job #${env.BUILD_NUMBER}"
                                git push origin main
                            """
                        } else {
                            echo "No services were changed. Skipping manifest update."
                        }

                        sh "cd .. && rm -rf manifests-repo"
                    }
                }
            }
        }
    }
    post {
        always {
            script {
                echo "빌드 완료: ${env.BUILD_NUMBER}"
            }
        }
        success {
            echo "✅ 파이프라인 성공! 매니페스트 리포지토리를 확인하세요."
        }
        failure {
            echo "❌ 파이프라인 실패!"
        }
    }
}

// 공통 빌드/푸시 함수
def buildAndPush(String serviceName, String servicePath, String image) {
    echo "Building ${serviceName} from path ${servicePath}..."
    dir(servicePath) {
        // Gradle 프로젝트인 경우 clean build 실행
        if (fileExists('build.gradle')) {
            sh "./gradlew clean build -x test"
        }
        sh "docker build -t ${image} ."
    }
    withAWS(credentials: AWS_CREDENTIALS_ID, region: AWS_DEFAULT_REGION) {
        sh "aws ecr get-login-password --region ${AWS_DEFAULT_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}"
        echo "Pushing ${image} to ECR..."
        sh "docker push ${image}"
    }
}

// 변경된 서비스 목록을 찾아주는 함수
@NonCPS
def findChangedServices() {
    // 마지막 성공 빌드 이후의 변경사항을 감지. 첫 빌드일 경우 HEAD~1 사용
    def sinceCommit = env.GIT_PREVIOUS_SUCCESSFUL_COMMIT ?: 'HEAD~1'
    def changedFiles = sh(script: "git diff --name-only ${sinceCommit} HEAD", returnStdout: true).trim().split('\n')
    def changedServices = new HashSet<String>()
    
    changedFiles.each { file ->
        if (file.startsWith("config/")) {
            def serviceName = file.split('/')[1]
            if (serviceName) changedServices.add(serviceName)
        } else if (file.startsWith("services/")) {
            def serviceName = file.split('/')[1]
            if (serviceName) changedServices.add(serviceName)
        }
    }
    return changedServices
}