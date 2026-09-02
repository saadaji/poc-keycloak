def pod =
"""
apiVersion: v1
kind: Pod
metadata:
 labels:
   name: worker
spec:
 serviceAccountName: jenkins
 containers:
   - name: java21
     image: maven:3.9-eclipse-temurin-21
     resources:
       requests:
         cpu: "1000m"
         memory: "2048Mi"
     imagePullPolicy: Always
     tty: true
     command: ["cat"]
   - name: dind
     image: docker:dind
     imagePullPolicy: Always
     tty: true
     env:
       - name: DOCKER_TLS_CERTDIR
         value: ""
     securityContext:
       privileged: true
   - name: terraform
     image: hashicorp/terraform:1.15
     imagePullPolicy: Always
     tty: true
     command: ["cat"]
"""
 
pipeline {
   agent {
       kubernetes {
           yaml pod
       }
   }

   options {
        buildDiscarder(logRotator(numToKeepStr: '15'))
        disableConcurrentBuilds()
        timeout(time: 60, unit: 'MINUTES')
    }

   environment {
       DOCKER_HOST = 'tcp://localhost:2375'
       DOCKER_TLS_VERIFY = 0
       TESTCONTAINERS_RYUK_DISABLED = 'true'
   }
 
   stages {

        stage('Debug') {
            steps {
                echo "BRANCH_NAME = ${env.BRANCH_NAME}"
                echo "TAG_NAME =  ${env.TAG_NAME}"
            }
        }

        stage('Prepare environment') {
            steps {
                script {
                    // variables used to build an deploy dockerfiles to ecr repositories
                    AWS_REGION = 'eu-west-3'
                    AWS_USER_ACCOUNT = "jenkins_techlead_dev"
                    //AWS_ACCOUNT = "529031532506"
                    //ECR_AWS_ACCOUNT = "873168974321"
                    //AWS_ROLE = "PATRIMOINE_SYSOPS_PRI"
                    keycloak_vef_codebuild_project_name = "keycloak-vef-registry-prd"
                }
            }
        }

       stage('Build and Test') {
           steps {
               container('java21') {
                   script {
                       // Restrict which Keycloak themes get packaged based on the git tag.
                       // If the tag contains a known theme keyword (case-insensitive),
                       // only that theme is bundled into keycloak-themes-*.jar; otherwise
                       // all themes are included (default behavior).
                       def tagForThemes = (env.TAG_NAME ?: '').toLowerCase()
                       def themeArg = ''
                       def themeKeywords = ['ileo', 'setom']
                       def matchedTheme = themeKeywords.find { tagForThemes.contains(it) }
                       if (matchedTheme) {
                           themeArg = "-Dtheme.include=${matchedTheme}"
                           echo "Tag '${env.TAG_NAME}' matches theme filter: building only '${matchedTheme}' theme"
                       }
                       sh "mvn clean install ${themeArg}"
                   }
               }
           }
       }
       stage('Docker Image Build') {
            steps {
                script{
                    TAG_VERSION = env.TAG_NAME
                    
                    if (!TAG_VERSION) {
                        TAG_VERSION = input(
                            message: 'Enter tag name for Docker image build:',
                            parameters: [
                                string(name: 'TAG', defaultValue: '', description: 'Docker image tag')
                            ]
                        )
                    }
                    
                    echo "TAG_NAME = ${TAG_VERSION}"

                    withAWS(credentials: "${AWS_USER_ACCOUNT}"){
                        echo keycloak_vef_codebuild_project_name
                        awsCodeBuild projectName: "${keycloak_vef_codebuild_project_name}", region: "${AWS_REGION}", sourceControlType: 'jenkins', credentialsType: 'keys', envVariables: "[{TAG, ${TAG_VERSION}}]"
                    }
                }
            }
        }

        stage('Update Terraform Stack') {
            steps {
                container('terraform') {
                    withCredentials([
                        string(credentialsId: 'KECYLOAK_INFRA_TOKEN', variable: 'KEYCLOAK_INFRA_TOKEN')
                    ]) {
                        catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                            withAWS(credentials: "${AWS_USER_ACCOUNT}") {
                                sh """
                                    GIT_REPO_URL="https://x-token-auth:\${KEYCLOAK_INFRA_TOKEN}@bitbucket.org/ist-efr/plf_clp_federation_id_keycloak.git"

                                    git clone "\${GIT_REPO_URL}" tf-stack
                                    cd tf-stack/infra/3_app

                                    sed -i 's|ecr_keycloak_image_version.*=.*|ecr_keycloak_image_version = "'"${TAG_VERSION}"'"|' env/dev/variables.tfvars

                                    terraform init -reconfigure -backend-config=env/dev/backend.conf
                                    terraform apply -auto-approve \\
                                        -var-file=env/dev/variables.tfvars

                                    git config user.email "jenkins@ci"
                                    git config user.name "Jenkins"
                                    git remote set-url origin "\${GIT_REPO_URL}"
                                    git add -A
                                    if git commit -m "chore: update keycloak image version to ${TAG_VERSION}"; then
                                        git push
                                    else
                                        echo "Nothing to commit, skipping push"
                                    fi
                                """
                            }
                        }
                    }
                }
            }
        }
   }
}