// Jenkinsfile: 서비스별 통합 YAML + 배포 순서 보장 최종 버전

// 빌드/배포 결과를 저장하기 위한 전역 변수
def buildResults = [succeeded: [], failed: []]
def changedServicePaths = []

pipeline {
    agent any

    environment {
        // AWS 설정
        AWS_DEFAULT_REGION = 'ap-northeast-2'
        AWS_ACCOUNT_ID = '883467884806'
        ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com"
        
        // 단일 ECR 리포지토리 이름
        UNIFIED_ECR_REPO = 'be09-final-1team-be'
        
        // Jenkins Credentials ID
        AWS_CREDENTIALS_ID = 'aws-credentials'
        GIT_CREDENTIALS_ID = 'BE09-Final-1team-k8s-manifests-ssh-key'

        // Kubernetes Manifests 리포지토리 정보
        MANIFEST_REPO_URL = 'git@github.com:backend20250319/BE09-Final-1team-k8s-manifests.git'
        
        // EKS 설정
        EKS_CLUSTER_NAME = 'BE09-Final-1team-BE-cluster'
        EKS_NAMESPACE = 'msa-namespace'

        // Docker 이미지 태그 (빌드번호 + 커밋해시)
        IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.take(7)}"
    }

    stages {
        stage('Detect Changed Services') {
            steps {
                script {
                    echo "Detecting changed services on Windows..."
                    def changedServices = new HashSet<String>()
                    
                    def servicePathsOutput = bat(returnStdout: true, script: 'dir /s /b Dockerfile').trim()
                    def allServicePaths = servicePathsOutput.split('\r\n').findAll { line -> !line.startsWith('>') && line.trim() != '' }.collect { it.replace('\\Dockerfile', '') }
                    
                    def workspacePath = env.WORKSPACE
                    def relativeServicePaths = allServicePaths.collect { it.replace(workspacePath, '').replaceAll('^\\\\', '') }
                    echo "Found all relative service paths: ${relativeServicePaths}"

                    if (currentBuild.changeSets.isEmpty()) {
                        echo "No changesets found. Building all services."
                        changedServices.addAll(relativeServicePaths)
                    } else {
                        for (changeSet in currentBuild.changeSets) {
                            for (item in changeSet.items) {
                                for (path in item.affectedPaths) {
                                    def windowsStyleFile = path.replace('/', '\\')
                                    for (String servicePath in relativeServicePaths) {
                                        if (windowsStyleFile.startsWith(servicePath + '\\')) {
                                            changedServices.add(servicePath)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (changedServices.isEmpty()) {
                        echo "No changes detected. Skipping subsequent stages."
                        currentBuild.result = 'NOT_BUILT'
                        return
                    }
                    
                    echo "Services to be built: ${changedServices.toList()}"
                    changedServicePaths = changedServices.toList()
                }
            }
        }

        stage('Build and Push Changed Services') {
            when { expression { !changedServicePaths.isEmpty() } }
            steps {
                script {
                    def parallelStages = [:]

                    changedServicePaths.each { servicePath ->
                        def currentService = servicePath
                        parallelStages["Build & Push ${currentService}"] = {
                            try {
                                def serviceName = currentService.split('\\\\').last()
                                def imageTag = "${IMAGE_TAG}"
                                buildAndPush(serviceName, currentService, imageTag)
                                buildResults.succeeded.add(serviceName)
                            } catch (e) {
                                echo "ERROR during build or push for ${currentService}: ${e.toString()}"
                                buildResults.failed.add(currentService.split('\\\\').last())
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

        stage('Deploy to EKS') {
            when { expression { !buildResults.succeeded.isEmpty() } }
            steps {
                script {
                    echo "Deploying successfully built services: ${buildResults.succeeded.join(', ')}"
                    
                    bat "aws eks update-kubeconfig --name ${EKS_CLUSTER_NAME} --region ${AWS_DEFAULT_REGION}"

                    withCredentials([sshUserPrivateKey(credentialsId: GIT_CREDENTIALS_ID, keyFileVariable: 'GIT_KEY')]) {
                        bat "git clone ${MANIFEST_REPO_URL} manifests-repo"
                    }

                    // 1. 서비스 의존성에 따른 배포 순서 정의
                    def deploymentOrder = [
                        'config-server',
                        'discovery-service',
                        'gateway-service',
                        'user-service',
                        'news-service',
                        'flaskapi',
                        'dedup-service',
                        'crawler-service',
                        'newsletter-service',
                        'tooltip-service'
                    ]

                    // 2. 전역 설정(Namespace, Secrets)을 먼저 적용
                    echo "Applying global manifests..."
                    bat "kubectl apply -f manifests-repo\\k8s-namespace.yml"
                    
                    // 시크릿 생성 (환경변수에서)
                    bat """
                        kubectl create secret generic db-secret ^
                          --from-literal=url="%DB_URL%" ^
                          --from-literal=username="%DB_USERNAME%" ^
                          --from-literal=password="%DB_PASSWORD%" ^
                          -n ${EKS_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
                        
                        kubectl create secret generic jwt-secret ^
                          --from-literal=secret="%JWT_SECRET%" ^
                          --from-literal=expiration="%JWT_EXPIRATION%" ^
                          -n ${EKS_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
                        
                        kubectl create secret generic mail-secret ^
                          --from-literal=host="%MAIL_HOST%" ^
                          --from-literal=port="%MAIL_PORT%" ^
                          --from-literal=username="%MAIL_USERNAME%" ^
                          --from-literal=password="%MAIL_PASSWORD%" ^
                          -n ${EKS_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
                    """

                    // 3. 정의된 순서대로, 빌드된 서비스만 골라서 배포
                    deploymentOrder.each { serviceName ->
                        if (buildResults.succeeded.contains(serviceName)) {
                            echo "--- Starting deployment for ${serviceName} (in order) ---"
                            def image = "${ECR_REGISTRY}/${UNIFIED_ECR_REPO}:${serviceName}-${IMAGE_TAG}"
                            def serviceManifestFile = "manifests-repo\\k8s-${serviceName}.yml"

                            // 매니페스트 파일에서 IMAGE_TAG 변수를 실제 이미지로 치환
                            bat "powershell -Command \"(Get-Content '${serviceManifestFile}') -replace '\\$\\{IMAGE_TAG\\}', '${IMAGE_TAG}' | Set-Content '${serviceManifestFile}'\""
                            bat "kubectl apply -f ${serviceManifestFile}"
                        }
                    }

                    // 4. 마지막으로 Ingress 설정을 적용
                    echo "Applying ingress manifest..."
                    bat "kubectl apply -f manifests-repo\\k8s-ingress.yml"
                }
            }
        }
    }
    
    post {
        always {
            script {
                echo '--- Summary ---'
                if (!buildResults.succeeded.isEmpty()) {
                    echo "✅ Succeeded builds: ${buildResults.succeeded.join(', ')}"
                }
                if (!buildResults.failed.isEmpty()) {
                    echo "❌ Failed builds: ${buildResults.failed.join(', ')}"
                }
                if (currentBuild.result == 'NOT_BUILT') {
                    echo "- No services were built as no changes were detected."
                }
                echo '---------------'
                
                cleanWs()
                bat "if exist manifests-repo ( rmdir /s /q manifests-repo )"
            }
        }
    }
}

// 공통 빌드/푸시 함수 (Windows / 통합 ECR 리포지토리 용)
def buildAndPush(String serviceName, String servicePath, String imageTag) {
    def image = "${ECR_REGISTRY}/${UNIFIED_ECR_REPO}:${serviceName}-${imageTag}"
    
    echo "Building ${serviceName} from path ${servicePath}..."
    dir(servicePath) {
        if (fileExists('gradlew.bat')) {
            bat "gradlew.bat clean build -x test --no-daemon"
        }
        bat "docker build -t ${image} ."
    }
    withCredentials([aws(credentialsId: AWS_CREDENTIALS_ID)]) {
        def loginCmd = "aws ecr get-login-password --region ${AWS_DEFAULT_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}"
        bat(script: loginCmd)
        echo "Pushing ${image} to ECR..."
        bat "docker push ${image}"
    }
}