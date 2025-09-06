// Jenkinsfile: Windows 경로 필터링 및 Gradle Daemon 비활성화 적용 버전

// 빌드/배포 결과를 저장하기 위한 전역 변수
def buildResults = [succeeded: [], failed: []]
def changedServicePaths = []

pipeline {
    agent any

    environment {
        // AWS 설정
        AWS_DEFAULT_REGION = 'ap-northeast-2'
        AWS_ACCOUNT_ID = '883467884806' // 본인 계정 ID
        ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com"
        
        // Jenkins Credentials ID
        AWS_CREDENTIALS_ID = 'aws-credentials'
        GIT_CREDENTIALS_ID = 'BE09-Final-1team-k8s-manifests-ssh-key' // Manifest Repo 접근용 SSH 키

        // Kubernetes Manifests 리포지토리 정보 (YAML 파일을 가져오기 위해 필요)
        // ▼▼▼ 여기에 Manifest 리포지토리의 SSH 주소를 꼭 채워주세요!!! ▼▼▼
        MANIFEST_REPO_URL = 'git@github.com:backend20250319/BE09-Final-1team-k8s-manifests.git' // 예시 주소입니다. 실제 주소로 변경하세요.
        
        // EKS 설정
        EKS_CLUSTER_NAME = 'BE09-Final-1team-BE-cluster'

        // Docker 이미지 태그 (모든 스테이지에서 동일한 태그 사용)
        IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.take(7)}"
    }

    stages {
        stage('Detect Changed Services') {
            steps {
                script {
                    echo "Detecting changed services on Windows..."
                    def changedServices = new HashSet<String>()
                    
                    def servicePathsOutput = bat(returnStdout: true, script: 'dir /s /b Dockerfile').trim()
                    
                    // [수정 1] 명령어 출력 결과에서 불필요한 라인을 필터링합니다
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
                                def image = "${ECR_REGISTRY}/${serviceName}:${IMAGE_TAG}"
                                buildAndPush(serviceName, currentService, image)
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

                    buildResults.succeeded.each { serviceName ->
                        echo "--- Starting deployment for ${serviceName} ---"
                        def image = "${ECR_REGISTRY}/${serviceName}:${IMAGE_TAG}"
                        
                        def deploymentFile = "manifests-repo\\${serviceName}-deployment.yaml"
                        def serviceFile = "manifests-repo\\${serviceName}-service.yaml"

                        bat "powershell -Command \"(Get-Content '${deploymentFile}') -replace 'image:.*', 'image: ${image}' | Set-Content '${deploymentFile}'\""

                        bat "kubectl apply -f ${deploymentFile}"
                        bat "kubectl apply -f ${serviceFile}"
                    }
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

// 공통 빌드/푸시 함수 (Windows 용)
def buildAndPush(String serviceName, String servicePath, String image) {
    echo "Building ${serviceName} from path ${servicePath}..."
    dir(servicePath) {
        if (fileExists('gradlew.bat')) {
            // [수정 2] Gradle Daemon 비활성화 옵션을 추가합니다
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