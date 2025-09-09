// Jenkinsfile: Argo CD 연동을 위한 최종 GitOps 버전 (모든 서비스 항상 빌드 - 필터링 로직 수정)

// 빌드/배포 결과를 저장하기 위한 전역 변수
def buildResults = [succeeded: [], failed: []]
def servicePathsToBuild = []

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
        stage('Detect All Services') {
            steps {
                script {
                    echo "Detecting all services to build..."
                    def allServices = new HashSet<String>()

                    def servicePathsOutput = bat(returnStdout: true, script: 'dir /s /b Dockerfile').trim()

                    // [수정됨] 필터링 로직 강화: 명령어 에코 등 불필요한 라인을 확실히 제거합니다.
                    // 실제 'Dockerfile' 텍스트를 포함하는 라인만 유효한 경로로 간주합니다.
                    def validServicePaths = servicePathsOutput.split('\r\n').findAll { line ->
                        line.trim() != '' && line.contains('Dockerfile')
                    }.collect { it.replace('\\Dockerfile', '') }

                    def workspacePath = env.WORKSPACE
                    def relativeServicePaths = validServicePaths.collect { it.replace(workspacePath, '').replaceAll('^\\\\', '') }
                    echo "Found all relative service paths: ${relativeServicePaths}"

                    echo "Pipeline configured to build all services on every run."
                    allServices.addAll(relativeServicePaths)

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

        stage('Build and Push All Services') {
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

                    dir('manifests-repo') {
                        buildResults.succeeded.each { serviceName ->
                            echo "--- Updating manifest for ${serviceName} ---"
                            def fullTag = "${serviceName}-${IMAGE_TAG}"
                            def image = "${ECR_REGISTRY}/${UNIFIED_ECR_REPO}:${fullTag}"
                            def serviceManifestFile = "k8s-${serviceName}.yml"

                            bat "powershell -Command \"(Get-Content '${serviceManifestFile}') -replace 'image:.*', 'image: ${image}' | Set-Content '${serviceManifestFile}'\""
                        }

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