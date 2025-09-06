// Jenkinsfile: Windows 환경 / Flat Manifest Repo 맞춤 최종 버전

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
        
        // Jenkins Credentials ID
        AWS_CREDENTIALS_ID = 'aws-credentials'
        GIT_CREDENTIALS_ID = 'BE09-Final-1team-k8s-manifests-ssh-key' // Manifest Repo 접근용 SSH 키

        // Kubernetes Manifests 리포지토리 정보 (YAML 파일을 가져오기 위해 필요)
        MANIFEST_REPO_URL = 'git@github.com:Berry-mas/BE09_Final_1team_k8s_manifests.git' // 예시 주소입니다. 실제 주소로 변경하세요.
        
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
                    
                    // 1. Windows에서 Dockerfile 기준으로 모든 서비스 경로 찾기
                    def servicePathsOutput = bat(returnStdout: true, script: 'dir /s /b Dockerfile').trim()
                    def allServicePaths = servicePathsOutput.split('\r\n').collect { it.replace('\\Dockerfile', '') }
                    
                    // Workspace 절대 경로를 제거하여 상대 경로로 변환
                    def workspacePath = env.WORKSPACE
                    def relativeServicePaths = allServicePaths.collect { it.replace(workspacePath, '').replaceAll('^\\\\', '') }
                    echo "Found all relative service paths: ${relativeServicePaths}"

                    // 2. 변경된 파일 목록을 기반으로 빌드가 필요한 서비스 경로 필터링
                    if (currentBuild.changeSets.isEmpty()) {
                        echo "No changesets found. Building all services."
                        changedServices.addAll(relativeServicePaths)
                    } else {
                        for (changeSet in currentBuild.changeSets) {
                            for (item in changeSet.items) {
                                for (path in item.affectedPaths) {
                                    // Git 변경 경로는 '/'를 사용하므로 Windows 스타일'\'로 변경
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
                                buildResults.failed.add(currentService.split('\\').last())
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
                        // Git for Windows가 설치된 환경에서 실행
                        bat "git clone ${MANIFEST_REPO_URL} manifests-repo"
                    }

                    buildResults.succeeded.each { serviceName ->
                        echo "--- Starting deployment for ${serviceName} ---"
                        def image = "${ECR_REGISTRY}/${serviceName}:${IMAGE_TAG}"
                        
                        // ▼▼▼ Manifest Repo에 폴더가 없는 구조에 맞게 파일 경로 수정 ▼▼▼
                        def deploymentFile = "manifests-repo\\${serviceName}-deployment.yaml"
                        def serviceFile = "manifests-repo\\${serviceName}-service.yaml"

                        // powershell을 사용하여 이미지 태그 변경 (sed 명령어 대체)
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
                // ... (요약 내용은 동일) ...
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
        // ▼▼▼ Windows 환경에 맞게 gradlew.bat 실행으로 변경 ▼▼▼
        if (fileExists('gradlew.bat')) {
            bat "gradlew.bat clean build -x test"
        }
        bat "docker build -t ${image} ."
    }
    withAWS(credentials: AWS_CREDENTIALS_ID, region: AWS_DEFAULT_REGION) {
        // withAWS 블록은 내부적으로 OS에 맞게 동작하므로 수정 불필요
        def loginCmd = "aws ecr get-login-password --region ${AWS_DEFAULT_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}"
        bat(script: loginCmd)

        echo "Pushing ${image} to ECR..."
        bat "docker push ${image}"
    }
}