// Jenkinsfile: AWS Secrets Manager 연동 및 모든 오류 수정 최종 버전

// 빌드/배포 결과를 저장하기 위한 전역 변수
def buildResults = [succeeded: [], failed: []]
def changedServicePaths = []

pipeline {
    agent any

    tools {
            jdk 'jdk17'
    }

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
                    def allServicePaths = servicePathsOutput.split('\r\n').findAll { line ->
                        line.matches("^[a-zA-Z]:.*") && !line.contains('>') && line.trim() != ''
                    }.collect { it.replace('\\Dockerfile', '') }

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
                            // ▼▼▼ [오류 수정] synchronized를 제거하고, 결과를 return 하도록 변경 ▼▼▼
                            def result = [:]
                            def serviceName = currentService.split('\\\\').last()
                            try {
                                def fullTag = "${serviceName}-${IMAGE_TAG}"
                                buildAndPush(serviceName, currentService, fullTag)
                                result = [service: serviceName, status: 'SUCCESS']
                            } catch (e) {
                                echo "ERROR in parallel stage for ${currentService}: ${e.getMessage()}"
                                result = [service: serviceName, status: 'FAILURE']
                            }
                            return result // 각 병렬 작업의 결과를 반환
                        }
                    }

                    // 모든 병렬 작업이 끝난 후, 반환된 결과들을 취합
                    def stageResults = parallel parallelStages

                    // 결과를 순차적으로 buildResults에 저장 (더 이상 동시성 문제 없음)
                    stageResults.each { stageName, result ->
                        if (result != null) {
                            if (result.status == 'SUCCESS') {
                                buildResults.succeeded.add(result.service)
                            } else {
                                buildResults.failed.add(result.service)
                            }
                        }
                    }

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
                        // ▼▼▼ [개선] git clone 시 SSH key를 사용하도록 수정 ▼▼▼
                        bat "set GIT_SSH_COMMAND=ssh -i %GIT_KEY% -o StrictHostKeyChecking=no && git clone ${MANIFEST_REPO_URL} manifests-repo"
                    }

                    def deploymentOrder = [
                        'config-server', 'discovery-service', 'gateway-service', 'user-service',
                        'news-service', 'flaskapi', 'dedup-service', 'crawler-service',
                        'newsletter-service', 'tooltip-service'
                    ]

                    echo "Applying global and prerequisite manifests..."
                    bat "kubectl apply -f manifests-repo\\k8s-namespace.yml"
                    bat "kubectl apply -f manifests-repo\\k8s-flaskapi-configmap.yml"
                    bat "for %%i in (manifests-repo\\k8s-*-spc.yml) do kubectl apply -f %%i"

                    deploymentOrder.each { serviceName ->
                        if (buildResults.succeeded.contains(serviceName)) {
                            echo "--- Starting deployment for ${serviceName} (in order) ---"
                            def fullTag = "${serviceName}-${IMAGE_TAG}"
                            def image = "${ECR_REGISTRY}/${UNIFIED_ECR_REPO}:${fullTag}"
                            def serviceManifestFile = "manifests-repo\\k8s-${serviceName}.yml"

                            bat "powershell -Command \"(Get-Content '${serviceManifestFile}') -replace 'image:.*', 'image: ${image}' | Set-Content '${serviceManifestFile}'\""

                            bat "kubectl apply -f ${serviceManifestFile}"
                        }
                    }

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

// 공통 빌드/푸시 함수 (Windows / 단일 ECR 리포지토리 용)
def buildAndPush(String serviceName, String servicePath, String fullTag) {
    def image = "${ECR_REGISTRY}/${UNIFIED_ECR_REPO}:${fullTag}"

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

