// Jenkinsfile: AWS Secrets Manager 연동 및 모든 오류 수정 최종 버전

// 빌드/배포 결과를 저장하기 위한 전역 변수
def buildResults = [succeeded: [], failed: []]
def changedServicePaths = []

pipeline {
    agent any

    // 젠킨스 Tools에 설정된 JDK를 사용하도록 명시
    tools {
        jdk 'jdk17'
    }

    environment {
        // AWS 설정
        AWS_DEFAULT_REGION = 'ap-northeast-2'
        AWS_ACCOUNT_ID = '783648732440'
        ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com"

        // 단일 ECR 리포지토리 이름
        UNIFIED_ECR_REPO = 'my-back'

        // Jenkins Credentials ID
        AWS_CREDENTIALS_ID = 'aws-credentials'
        GIT_CREDENTIALS_ID = 'BE09-Final-1team-k8s-manifests-ssh-key'

        // Kubernetes Manifests 리포지토리 정보
        MANIFEST_REPO_URL = 'git@github.com:Berry-mas/my-k8s.git'

        // EKS 설정
        EKS_CLUSTER_NAME = 'my-msa-cluster'
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

                    // Windows에서 안정적으로 Dockerfile 경로를 찾는 방식으로 수정
                    def findDockerfilesCmd = 'where /r . Dockerfile'
                    def allServicePathsOutput = bat(returnStdout: true, script: findDockerfilesCmd).trim()
                    def allServicePaths = allServicePathsOutput.split('\r\n').findAll { it.trim() != '' }.collect { it.replace('\\Dockerfile', '') }

                    def workspacePath = pwd().replace('/', '\\')
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
                                def fullTag = "${serviceName}-${IMAGE_TAG}"
                                buildAndPush(serviceName, currentService, fullTag)
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

