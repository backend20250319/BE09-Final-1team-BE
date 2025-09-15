// Jenkinsfile: manifests 구조 개편 반영 + 변경 서비스만 롤아웃

def buildResults = [succeeded: [], failed: []]
def changedServicePaths = []

pipeline {
  agent any

  tools {
    jdk 'jdk17'
  }

  parameters {
    booleanParam(name: 'FORCE_FULL_BUILD', defaultValue: false, description: 'Build all services regardless of changes')
  }

  environment {
    AWS_DEFAULT_REGION = 'ap-northeast-2'
    AWS_ACCOUNT_ID     = '783648732440'
    ECR_REGISTRY       = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com"

    UNIFIED_ECR_REPO   = 'my-back'

    AWS_CREDENTIALS_ID = 'aws-credentials'
    GIT_CREDENTIALS_ID = 'BE09-Final-1team-k8s-manifests-ssh-key'

    MANIFEST_REPO_URL  = 'git@github.com:Berry-mas/my-k8s.git'
    MANIFEST_REPO_DIR  = 'manifests-repo'

    EKS_CLUSTER_NAME   = 'my-msa-cluster'
    EKS_NAMESPACE      = 'msa-namespace'

    IMAGE_TAG          = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.take(7)}"
  }

  stages {

    stage('Detect Changed Services') {
      steps {
        script {
          echo "Detecting changed services on Windows..."
          def changedServices = new HashSet<String>()

          // 모든 Dockerfile 경로 검색
          def allServicePathsOutput = bat(returnStdout: true, script: 'where /r . Dockerfile').trim()
          def allServicePaths = allServicePathsOutput.split('\r\n').findAll { line ->
            line.trim() && !line.startsWith('>') && line.contains('\\Dockerfile')
          }.collect { it.replace('\\Dockerfile', '') }

          // 워크스페이스 상대 경로로
          def ws = pwd().replace('/', '\\')
          def relPaths = allServicePaths.collect { it.replace(ws, '').replaceAll('^\\\\', '') }
          echo "Found all service paths: ${relPaths}"

          if (params.FORCE_FULL_BUILD || currentBuild.number == 1) {
            echo params.FORCE_FULL_BUILD ? "FORCE_FULL_BUILD=true → all services" : "First build → all services"
            changedServices.addAll(relPaths)
          } else {
            def diff = bat(returnStdout: true, script: 'git diff --name-only HEAD~1 HEAD').trim()
            def changedFiles = diff.split('\r\n').findAll { it?.trim() && !it.contains('git diff --name-only') }
            echo "Changed files in last commit: ${changedFiles}"

            for (String f : changedFiles) {
              def w = f.replace('/', '\\')
              for (String svcPath : relPaths) {
                if (w.startsWith(svcPath + '\\')) changedServices.add(svcPath)
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
                def fullTag     = "${serviceName}-${IMAGE_TAG}"
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
            bat "aws eks update-kubeconfig --name ${EKS_CLUSTER_NAME} --region ${AWS_DEFAULT_REGION}"

            // Git for Windows의 ssh/ssh-agent 우선
            withEnv(['PATH=C:\\Program Files\\Git\\usr\\bin;C:\\Program Files\\Git\\mingw64\\bin;%PATH%']) {
              bat 'where ssh && ssh -V && git --version'

              // manifests repo 체크아웃
              sshagent(credentials: [GIT_CREDENTIALS_ID]) {
                bat "if exist ${MANIFEST_REPO_DIR} ( rmdir /s /q ${MANIFEST_REPO_DIR} )"
                bat "git clone ${MANIFEST_REPO_URL} ${MANIFEST_REPO_DIR}"
              }
            }

            // --- 매니페스트 적용 순서 (새 구조에 맞춤) ---
            // 1) Namespace / ServiceAccount(IRSA)
            bat "kubectl apply -f ${MANIFEST_REPO_DIR}\\k8s-namespace.yml"
            bat "kubectl apply -f ${MANIFEST_REPO_DIR}\\k8s-service-account.yml"

            // 2) Configs (ConfigMap 등) - 파일이 없을 수도 있으니 존재 시만
            bat "if exist ${MANIFEST_REPO_DIR}\\k8s-flaskapi-configmap.yml kubectl apply -f ${MANIFEST_REPO_DIR}\\k8s-flaskapi-configmap.yml"
            bat "if exist ${MANIFEST_REPO_DIR}\\k8s-dedup-service-configmap.yml kubectl apply -f ${MANIFEST_REPO_DIR}\\k8s-dedup-service-configmap.yml"

            // (선행 필요 시) Config Server
            bat "if exist ${MANIFEST_REPO_DIR}\\k8s-config-server.yml kubectl apply -f ${MANIFEST_REPO_DIR}\\k8s-config-server.yml"

            // 3) SecretProviderClass들 (CSI Driver/Provider 설치되어 있어야 함)
            bat "for %i in (${MANIFEST_REPO_DIR}\\k8s-*-spc.yml) do kubectl apply -f %i"

            // 4) Services
            bat "kubectl apply -f ${MANIFEST_REPO_DIR}\\k8s-all-services.yml"

            // 5) Deployments (전체 정의 적용)
            bat "kubectl apply -f ${MANIFEST_REPO_DIR}\\k8s-all-deployments.yml"

            // 6) 변경된 서비스만 이미지 태그 업데이트 + 롤아웃 대기
            buildResults.succeeded.each { svc ->
              def fullTag = "${svc}-${IMAGE_TAG}"
              def image   = "${ECR_REGISTRY}/${UNIFIED_ECR_REPO}:${fullTag}"

              // 우선 label app=<svc> 로 디플로이먼트 찾기 → 없으면 이름 추정으로 보조
              def ps = """
                \$ErrorActionPreference='Stop';
                \$ns='${EKS_NAMESPACE}';
                \$svc='${svc}';
                \$img='${image}';

                \$names = (kubectl -n \$ns get deploy -l app=\$svc -o jsonpath='{.items[*].metadata.name}');
                if([string]::IsNullOrEmpty(\$names)){
                  # fallback 이름 가설
                  \$candidates = @('${svc}','${svc}-deployment');
                  foreach(\$cand in \$candidates){
                    \$exists = (kubectl -n \$ns get deploy \$cand --ignore-not-found -o name);
                    if(-not [string]::IsNullOrEmpty(\$exists)){ \$names=\$cand; break }
                  }
                }

                if([string]::IsNullOrEmpty(\$names)){
                  Write-Host "[WARN] No deployment found for service '\$svc'. Skip set image.";
                  exit 0
                }

                foreach(\$d in \$names.Split(' ')){
                  if([string]::IsNullOrWhiteSpace(\$d)){ continue }
                  Write-Host "→ updating image for deployment/\$d to \$img";
                  kubectl -n \$ns set image deployment/\$d *=\$img --record
                  kubectl -n \$ns rollout status deployment/\$d --timeout=180s
                }
              """.stripIndent()

              bat "powershell -NoProfile -Command \"${ps.replace('\"','\\\"').replace('\n','; ')}\""
            }

            // 7) Ingress
            bat "kubectl apply -f ${MANIFEST_REPO_DIR}\\k8s-ingress.yml"
          }
        }
      }
    }
  }

  post {
    always {
      script {
        echo '--- Summary ---'
        if (!buildResults.succeeded.isEmpty()) echo "✅ Succeeded builds: ${buildResults.succeeded.join(', ')}"
        if (!buildResults.failed.isEmpty())    echo "❌ Failed builds: ${buildResults.failed.join(', ')}"
        if (currentBuild.result == 'NOT_BUILT') echo "- No services were built as no changes were detected."
        echo '---------------'

        cleanWs()
        bat "if exist ${MANIFEST_REPO_DIR} ( rmdir /s /q ${MANIFEST_REPO_DIR} )"
      }
    }
  }
}

// --- helpers ---
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
    bat "aws ecr get-login-password --region ${AWS_DEFAULT_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}"
    echo "Pushing ${image} to ECR..."
    bat "docker push ${image}"
  }
}
