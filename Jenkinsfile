// Jenkinsfile: Windows 노드용 (sshagent 미사용, GIT_SSH_COMMAND로 SSH 키 직접 사용)
// - 변경된 서비스만 빌드/푸시
// - manifests repo는 SSH 키로 클론
// - 매니페스트 구조: k8s-namespace.yml, k8s-service-account.yml,
//   k8s-*-spc.yml, k8s-config-server.yml, k8s-dedup-service-configmap.yml,
//   k8s-flaskapi-configmap.yml, k8s-all-services.yml, k8s-all-deployments.yml, k8s-ingress.yml

def buildResults = [succeeded: [], failed: []]
def changedServicePaths = []

pipeline {
  agent any
  tools { jdk 'jdk17' }

  parameters {
    booleanParam(name: 'FORCE_FULL_BUILD', defaultValue: false, description: 'Build all services regardless of changes')
  }

  environment {
    AWS_DEFAULT_REGION = 'ap-northeast-2'
    AWS_ACCOUNT_ID     = '783648732440'
    ECR_REGISTRY       = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com"
    UNIFIED_ECR_REPO   = 'my-back'

    AWS_CREDENTIALS_ID = 'aws-credentials'
    GIT_CREDENTIALS_ID = 'BE09-Final-1team-k8s-manifests-ssh-key' // SSH Username with private key

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

          def allServicePathsOutput = bat(returnStdout: true, script: 'where /r . Dockerfile').trim()
          def allServicePaths = allServicePathsOutput.split('\r\n').findAll { line ->
            line?.trim() && line.contains('\\Dockerfile')
          }.collect { it.replace('\\Dockerfile', '') }

          def ws = pwd().replace('/', '\\')
          def relPaths = allServicePaths.collect { it.replace(ws, '').replaceAll('^\\\\', '') }
          echo "Found all service paths: ${relPaths}"

          if (params.FORCE_FULL_BUILD || currentBuild.number == 1) {
            changedServices.addAll(relPaths)
            echo params.FORCE_FULL_BUILD ? 'FORCE_FULL_BUILD=true → all services' : 'First build → all services'
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
            echo 'No changes detected in service directories. Skipping subsequent stages.'
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
                echo "ERROR during build or push for ${currentService}: ${e}"
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

            // Git for Windows ssh.exe가 먼저 잡히도록 PATH 프리펜드
            def gitPaths = 'C:\\\\Program Files\\\\Git\\\\usr\\\\bin;C:\\\\Program Files\\\\Git\\\\mingw64\\\\bin'
            withEnv(["PATH=${gitPaths};${env.PATH}"]) {

              bat 'where ssh'
              bat 'ssh -V'
              bat 'git --version'

              // 🔑 SSH 키 파일 직접 사용 (sshagent 미사용)
              withCredentials([sshUserPrivateKey(credentialsId: GIT_CREDENTIALS_ID, keyFileVariable: 'GIT_KEY')]) {
                // 주의: 아래 withEnv 값은 Groovy가 아닌 CMD에서 %GIT_KEY%로 확장됨
                withEnv([
                  // exe 경로에 공백 있으므로 따옴표 필수
                  'GIT_SSH_COMMAND="C:\\Program Files\\Git\\usr\\bin\\ssh.exe" -o StrictHostKeyChecking=no -o UserKnownHostsFile=NUL -o IdentitiesOnly=yes -i "%GIT_KEY%"'
                ]) {
                  bat "if exist ${MANIFEST_REPO_DIR} ( rmdir /s /q ${MANIFEST_REPO_DIR} )"
                  bat "git clone ${MANIFEST_REPO_URL} ${MANIFEST_REPO_DIR}"
                }
              }

              // 네임스페이스/SA/ConfigMap/ConfigServer
              bat "kubectl apply -f ${MANIFEST_REPO_DIR}\\k8s-namespace.yml"
              bat "if exist ${MANIFEST_REPO_DIR}\\k8s-service-account.yml kubectl apply -f ${MANIFEST_REPO_DIR}\\k8s-service-account.yml"
              bat "if exist ${MANIFEST_REPO_DIR}\\k8s-flaskapi-configmap.yml kubectl apply -f ${MANIFEST_REPO_DIR}\\k8s-flaskapi-configmap.yml"
              bat "if exist ${MANIFEST_REPO_DIR}\\k8s-dedup-service-configmap.yml kubectl apply -f ${MANIFEST_REPO_DIR}\\k8s-dedup-service-configmap.yml"
              bat "if exist ${MANIFEST_REPO_DIR}\\k8s-config-server.yml kubectl apply -f ${MANIFEST_REPO_DIR}\\k8s-config-server.yml"

              // SecretProviderClass (CSI Driver 선행 설치되어 있어야 함)
              bat "for %i in (${MANIFEST_REPO_DIR}\\k8s-*-spc.yml) do kubectl apply -f %i"

              // Services / Deployments (묶음 파일)
              bat "kubectl apply -f ${MANIFEST_REPO_DIR}\\k8s-all-services.yml"
              bat "kubectl apply -f ${MANIFEST_REPO_DIR}\\k8s-all-deployments.yml"

              // 변경된 서비스만 이미지 교체 + 롤아웃
              buildResults.succeeded.each { svc ->
                def fullTag = "${svc}-${IMAGE_TAG}"
                def image   = "${ECR_REGISTRY}/${UNIFIED_ECR_REPO}:${fullTag}"
                def ps = """
                  \$ErrorActionPreference='Stop';
                  \$ns='${EKS_NAMESPACE}';
                  \$svc='${svc}';
                  \$img='${image}';

                  \$names = (kubectl -n \$ns get deploy -l app=\$svc -o jsonpath='{.items[*].metadata.name}');
                  if([string]::IsNullOrEmpty(\$names)){
                    \$candidates = @('${svc}','${svc}-deployment');
                    foreach(\$cand in \$candidates){
                      \$exists = (kubectl -n \$ns get deploy \$cand --ignore-not-found -o name);
                      if(-not [string]::IsNullOrEmpty(\$exists)){ \$names=\$cand; break }
                    }
                  }
                  if([string]::IsNullOrEmpty(\$names)){ Write-Host "[WARN] No deployment for '\$svc'"; exit 0 }

                  foreach(\$d in \$names.Split(' ')){
                    if([string]::IsNullOrWhiteSpace(\$d)){ continue }
                    Write-Host "→ set image deployment/\$d *=\$img";
                    kubectl -n \$ns set image deployment/\$d *=\$img --record
                    kubectl -n \$ns rollout status deployment/\$d --timeout=180s
                  }
                """.stripIndent()
                bat "powershell -NoProfile -Command \"${ps.replace('\"','\\\"').replace('\n','; ')}\""
              }

              // Ingress
              bat "if exist ${MANIFEST_REPO_DIR}\\k8s-ingress.yml kubectl apply -f ${MANIFEST_REPO_DIR}\\k8s-ingress.yml"
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
        if (!buildResults.succeeded.isEmpty())  echo "✅ Succeeded builds: ${buildResults.succeeded.join(', ')}"
        if (!buildResults.failed.isEmpty())     echo "❌ Failed builds: ${buildResults.failed.join(', ')}"
        if (currentBuild.result == 'NOT_BUILT') echo "- No services were built as no changes were detected."
        echo '---------------'
        cleanWs()
        bat "if exist ${MANIFEST_REPO_DIR} ( rmdir /s /q ${MANIFEST_REPO_DIR} )"
      }
    }
  }
}

// ---------------- helpers ----------------
def buildAndPush(String serviceName, String servicePath, String fullTag) {
  def image = "${ECR_REGISTRY}/${UNIFIED_ECR_REPO}:${fullTag}"
  echo "Building ${serviceName} from path ${servicePath}..."
  dir(servicePath) {
    if (fileExists('gradlew.bat')) { bat "gradlew.bat clean build -x test --no-daemon" }
    bat "docker build -t ${image} ."
  }
  withCredentials([aws(credentialsId: AWS_CREDENTIALS_ID)]) {
    bat "aws ecr get-login-password --region ${AWS_DEFAULT_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}"
    echo "Pushing ${image} to ECR..."
    bat "docker push ${image}"
  }
}
