// Jenkinsfile: Argo CD 연동을 위한 최종 GitOps 버전 (모든 서비스 항상 빌드)

// 빌드/배포 결과를 저장하기 위한 전역 변수
def buildResults = [succeeded: [], failed: []]
def servicePathsToBuild = [] // 변수 이름 변경 (changed -> toBuild)

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
        MANIFEST_REPO_URL = 'git@github.com:Berry-mas/BE09_Final_1team_k8s_manifests.git'

        // Docker 이미지 태그 (빌드번호 + 커밋해시)
        IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.take(7)}"
    }

    stages {
        stage('Detect All Services') { // 스테이지 이름 변경
            steps {
            script {
                echo "Detecting all services to build..."
                    def allServices = new HashSet<String>()

                    // 1. Dockerfile을 기준으로 모든 서비스의 경로를 찾습니다.
                    def servicePathsOutput = bat(returnStdout: true, script: 'dir /s /b Dockerfile').trim()
                    def allServicePaths = servicePathsOutput.split('\r\n').findAll { line -> !line.startsWith('>') && line.trim() != '' }.collect { it.replace('\\Dockerfile', '') }

                    def workspacePath = env.WORKSPACE
                    def relativeServicePaths = allServicePaths.collect { it.replace(workspacePath, '').replaceAll('^\\\\', '') }
                    echo "Found all relative service paths: ${relativeServicePaths}"

                    // 2. [수정됨] Git 변경사항을 확인하는 로직을 제거하고, 찾은 모든 서비스를 빌드 목록에 추가합니다.
                    echo "Pipeline configured to build all services on every run."
                    allServices.addAll(relativeServicePaths)

                    // 3. 빌드할 서비스가 없는 경우를 대비한 안전장치
                    if (allServices.isEmpty()) {
                    echo "No services with a Dockerfile were found. Skipping subsequent stages."
                        currentBuild.result = 'NOT_BUILT'
                        return
                    }

                    echo "Services to be built: ${allServices.toList()}"
                    servicePathsToBuild = allServices.toList()
                }
            }
        }

        stage('Build and Push All Services') { // 스테이지 이름 변경
            when { expression { !servicePathsToBuild.isEmpty() } }
            steps {
            script {
                def parallelStages = [:]

                    servicePathsToBuild.each { servicePath ->
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

        stage('Update Manifests and Push') {
            when { expression { !buildResults.succeeded.isEmpty() } }
            steps {
                script {
                    echo "Updating manifests for successfully built services: ${buildResults.succeeded.join(', ')}"

                    // 1. Manifest 리포지토리를 'manifests-repo' 폴더에 checkout
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: '*/main']],
                        userRemoteConfigs: [[
                            url: MANIFEST_REPO_URL,
                            credentialsId: GIT_CREDENTIALS_ID
                        ]],
                        extensions: [[
                            $class: 'RelativeTargetDirectory',
                            relativeTargetDir: 'manifests-repo'
                        ]]
                    ])

                    // 2. checkout 받은 폴더로 이동하여 작업
                    dir('manifests-repo') {
                        // 3. 빌드 성공한 모든 서비스들의 YAML 파일의 이미지 태그 교체
                        buildResults.succeeded.each { serviceName ->
                            echo "--- Updating manifest for ${serviceName} ---"
                            def fullTag = "${serviceName}-${IMAGE_TAG}"
                            def image = "${ECR_REGISTRY}/${UNIFIED_ECR_REPO}:${fullTag}"
                            def serviceManifestFile = "k8s-${serviceName}.yml"

                            bat "powershell -Command \"(Get-Content '${serviceManifestFile}') -replace 'image:.*', 'image: ${image}' | Set-Content '${serviceManifestFile}'\""
                        }

                        // 4. 변경된 파일들을 Git에 Commit 하고 Push
                        echo "Pushing updated manifests to Git repository..."
                        withCredentials([sshUserPrivateKey(credentialsId: GIT_CREDENTIALS_ID, keyFileVariable: 'GIT_KEY')]) {
                            bat """
                                set GIT_SSH_COMMAND=ssh -i "%GIT_KEY%" -o StrictHostKeyChecking=no
                                git config --global user.email "jenkins@example.com"
                                git config --global user.name "Jenkins CI"
                                git add .
                                git commit -m "Deploy: Update image tags for all services [Build #${env.BUILD_NUMBER}]"
                                git push origin HEAD:main
                            """
                        }
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
                    echo "- No services were built as no services with a Dockerfile were found."
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