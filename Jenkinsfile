// Jenkinsfile: 키파일 직접 지정 방식(ssh-agent 미사용) 최종 안정화 버전

// 빌드/배포 결과를 저장하기 위한 전역 변수
def buildResults = [succeeded: [], failed: []]
def changedServicePaths = []

pipeline {
    agent any

    // 젠킨스 Tools에 설정된 JDK를 사용하도록 명시
    tools {
        jdk 'jdk17'
    }

    parameters {
        booleanParam(name: 'FORCE_FULL_BUILD', defaultValue: false, description: 'Check this to build all services, regardless of changes.')
    }

    environment {
        // AWS 설정
        AWS_DEFAULT_REGION = 'ap-northeast-2'
        AWS_ACCOUNT_ID     = '783648732440'
        ECR_REGISTRY       = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com"

        // 단일 ECR 리포지토리 이름
        UNIFIED_ECR_REPO   = 'my-back'

        // Jenkins Credentials ID
        AWS_CREDENTIALS_ID = 'aws-credentials'
        GIT_CREDENTIALS_ID = 'BE09-Final-1team-k8s-manifests-ssh-key'  // SSH Username with private key

        // Kubernetes Manifests 리포지토리 정보 (SSH URL)
        MANIFEST_REPO_URL  = 'git@github.com:Berry-mas/my-k8s.git'

        // EKS 설정
        EKS_CLUSTER_NAME   = 'my-msa-cluster'
        EKS_NAMESPACE      = 'msa-namespace'

        // Docker 이미지 태그 (빌드번호 + 커밋해시7)
        IMAGE_TAG          = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.take(7)}"
    }

    stages {
        stage('Detect Changed Services') {
            steps {
                script {
                    echo "Detecting changed services on Windows..."
                    def changedServices = new HashSet<String>()

                    // 모든 Dockerfile 경로 검색
                    def findDockerfilesCmd = 'where /r . Dockerfile'
                    def allServicePathsOutput = bat(returnStdout: true, script: findDockerfilesCmd).trim()

                    def allServicePaths = allServicePathsOutput.split('\r\n').findAll { line ->
                        return line.trim() != '' && !line.startsWith('>') && line.contains('\\Dockerfile')
                    }.collect { it.replace('\\Dockerfile', '') }

                    // 워크스페이스 상대 경로로 정규화
                    def workspacePath = pwd().replace('/', '\\')
                    def relativeServicePaths = allServicePaths.collect { it.replace(workspacePath, '').replaceAll('^\\\\', '') }
                    echo "Found all service paths: ${relativeServicePaths}"

                    if (params.FORCE_FULL_BUILD) {
                        echo "FORCE_FULL_BUILD is checked. Building all services."
                        changedServices.addAll(relativeServicePaths)
                    } else if (currentBuild.number == 1) {
                        echo "First build. Building all services."
                        changedServices.addAll(relativeServicePaths)
                    } else {
                        // 마지막 커밋 기준 변경 파일 감지
                        def changedFilesOutput = bat(returnStdout: true, script: 'git diff --name-only HEAD~1 HEAD').trim()
                        def changedFiles = changedFilesOutput.split('\r\n').findAll { line ->
                            def t = line.trim()
                            return t != '' && !t.contains('git diff --name-only')
                        }
                        echo "Changed files in last commit: ${changedFiles}"

                        for (String file in changedFiles) {
                            def windowsStyleFile = file.replace('/', '\\')
                            for (String servicePath in relativeServicePaths) {
                                if (windowsStyleFile.startsWith(servicePath + '\\')) {
                                    changedServices.add(servicePath)
                                }
                            }
                        }
                    }

                    if (changedServices.isEmpty()) {
                        echo "No changes detected in service directories. Skipping subsequent stages."
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
                                def fullTag    = "${serviceName}-${IMAGE_TAG}"
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
            withCredentials([aws(credentialsId: AWS_CREDENTIALS_ID)]) {
              script {
                echo "Deploying successfully built services: ${buildResults.succeeded.join(', ')}"

                // kubeconfig 설정 (Windows 서비스 계정 프로필에 저장)
                bat "aws eks update-kubeconfig --name ${EKS_CLUSTER_NAME} --region ${AWS_DEFAULT_REGION}"

                // ✅ ssh-agent 대신, 키파일 직접 지정 방식 (환경변수 재확장 문제 회피용)
                withCredentials([sshUserPrivateKey(credentialsId: GIT_CREDENTIALS_ID, keyFileVariable: 'GIT_KEY')]) {
                  withEnv([
                    // Git for Windows 경로를 기존 PATH에 "추가" (덮어쓰기 금지)
                    'PATH+GIT=C:\\Program Files\\Git\\usr\\bin;C:\\Program Files\\Git\\mingw64\\bin',
                    // 드물게 cmd 탐색 문제 예방
                    'COMSPEC=C:\\Windows\\System32\\cmd.exe'
                  ]) {
                    // 멀티라인 배치로 한번에 처리 (여기서 %GIT_KEY%가 '즉시 확장'됨)
                    bat '''
                      @echo off
                      chcp 65001 >NUL

                      echo [INFO] Checking tools...
                      where ssh
                      ssh -V
                      git --version

                      rem 키 파일 존재 확인 (디버그용)
                      for %%I in ("%GIT_KEY%") do (
                        if not exist "%%~I" (
                          echo [ERROR] SSH key file not found: %%~I
                          exit /b 2
                        ) else (
                          echo [INFO] Using SSH key: %%~I
                        )
                      )

                      rem ★ 여기서 %GIT_KEY%가 즉시 확장됨
                      set "GIT_SSH_COMMAND=ssh -o StrictHostKeyChecking=no -i \"%GIT_KEY%\""

                      echo [INFO] Cloning manifests repo...
                      git clone git@github.com:Berry-mas/my-k8s.git manifests-repo
                      if errorlevel 1 exit /b 3
                    '''
                  }
                }

                // ──────────────────────────────
                // 매니페스트 적용 (공통/선행 리소스)
                // ──────────────────────────────
                echo "Applying global and prerequisite manifests..."
                bat "kubectl apply -f manifests-repo\\k8s-namespace.yml"
                bat "kubectl apply -f manifests-repo\\k8s-flaskapi-configmap.yml"
                bat "for %%i in (manifests-repo\\k8s-*-spc.yml) do kubectl apply -f %%i"

                // ──────────────────────────────
                // 서비스별 롤아웃 (이번 빌드에서 성공한 서비스만)
                // ──────────────────────────────
                def deploymentOrder = [
                  'config-server', 'discovery-service', 'gateway-service', 'user-service',
                  'news-service', 'flaskapi', 'dedup-service', 'crawler-service',
                  'newsletter-service', 'tooltip-service'
                ]

                deploymentOrder.each { serviceName ->
                  if (buildResults.succeeded.contains(serviceName)) {
                    echo "--- Starting deployment for ${serviceName} ---"
                    def fullTag = "${serviceName}-${IMAGE_TAG}"
                    def image   = "${ECR_REGISTRY}/${UNIFIED_ECR_REPO}:${fullTag}"
                    def serviceManifestFile = "manifests-repo\\k8s-${serviceName}.yml"

                    // image 라인 치환 (형식이 바뀌면 실패할 수 있으므로 유지보수에 유의)
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
