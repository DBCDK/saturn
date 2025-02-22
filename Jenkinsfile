#!groovy

def workerNode = "devel11"

pipeline {
	agent {label workerNode}
	tools {
		// refers to the name set in manage jenkins -> global tool configuration
		maven "Maven 3"
	}
	environment {
		DOCKER_TAG = "${env.BRANCH_NAME}-${env.BUILD_NUMBER}"
		GITLAB_PRIVATE_TOKEN = credentials("metascrum-gitlab-api-token")
	}
	triggers {
		upstream(upstreamProjects: "Docker-payara6-bump-trigger",
			threshold: hudson.model.Result.SUCCESS)
		pollSCM("H/03 * * * *")
	}
	options {
		timestamps()
		disableConcurrentBuilds(abortPrevious: true)
	}
	stages {
		stage("build") {
			steps {
				withSonarQubeEnv(installationName: 'sonarqube.dbc.dk') {
					script {
						def sonarOptions = "-Dsonar.branch.name=$BRANCH_NAME"
						if (env.BRANCH_NAME != 'master') {
							sonarOptions += " -Dsonar.newCode.referenceBranch=master"
						}

						def status = sh returnStatus: true, script: """
                            rm -rf $WORKSPACE/.repo/dk/dbc
                        """

						for (def goal : [ 'clean', "-Dtag=${BRANCH_NAME}-${BUILD_NUMBER} verify", "${sonarOptions} sonar:sonar" ]) {
							status += sh returnStatus: true, script: """
                                mvn -B -Dmaven.repo.local=$WORKSPACE/.repo --no-transfer-progress ${goal}
                            """
						}

						junit testResults: '**/target/*-reports/*.xml'

						if (status != 0) {
							error("build failed")
						}
					}
				}
			}
		}
		stage("quality gate") {
			steps {
				timeout(time: 1, unit: 'HOURS') {
					waitForQualityGate abortPipeline: true
				}
			}
		}
		stage("docker push") {
            when {
                branch "master"
            }
            steps {
                script {
                    sh """
                        docker push docker-metascrum.artifacts.dbccloud.dk/saturn-service:${BRANCH_NAME}-${BUILD_NUMBER}
                    """
                }
            }
        }
		stage("update staging version") {
			agent {
				docker {
					label workerNode
					image "docker-dbc.artifacts.dbccloud.dk/build-env"
					alwaysPull true
				}
			}
			when {
				branch "master"
			}
			steps {
				dir("deploy") {
					sh """
						set-new-version dataio-saturn-service.yml ${env.GITLAB_PRIVATE_TOKEN} metascrum/saturn-deploy ${env.DOCKER_TAG} -b staging
					"""
				}
			}
		}
	}
}
