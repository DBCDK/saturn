#!groovy

@Library("metascrum@master") _

def workerNode = "devel8"

pipeline {
	agent {label workerNode}
	tools {
		// refers to the name set in manage jenkins -> global tool configuration
		maven "Maven 3"
	}
	environment {
		DOCKER_TAG = "${env.BRANCH_NAME}-${env.BUILD_NUMBER}"
		MARATHON_TOKEN = credentials("METASCRUM_MARATHON_TOKEN")
		SONARQUBE_HOST = "http://sonarqube.mcp1.dbc.dk"
		SONARQUBE_TOKEN = credentials("dataio-sonarqube")
	}
	triggers {
		pollSCM("H/03 * * * *")
	}
	options {
		timestamps()
	}
	stages {
		stage("verify") {
			steps {
				sh "mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent verify pmd:pmd"
				junit "**/target/surefire-reports/TEST-*.xml,**/target/failsafe-reports/TEST-*.xml"
			}
		}
		stage("warnings") {
			steps {
				warnings consoleParsers: [
					[parserName: "Java Compiler (javac)"],
					[parserName: "JavaDoc Tool"]],
					unstableTotalAll: "0",
					failedTotalAll: "0"
			}
		}
		stage("pmd") {
			steps {
				step([$class: 'hudson.plugins.pmd.PmdPublisher',
					  pattern: '**/target/pmd.xml',
					  unstableTotalAll: "0",
					  failedTotalAll: "0"])
			}
		}
		stage("docker build") {
			steps {
				script {
					def image = docker.build("docker-io.dbc.dk/saturn-service:${env.DOCKER_TAG}")
					image.push()
				}
			}
		}
		stage("sonarqube") {
			when {
				branch "master"
			}
			steps {
				script {
					try {
						sh """
							mvn sonar:sonar \
							-Dsonar.host.url=$SONARQUBE_HOST \
							-Dsonar.login=$SONARQUBE_TOKEN
						"""
					} catch(e) {
						printf "sonarqube connection failed: %s", e.toString()
					}
				}
			}
		}
		stage("deploy staging") {
			agent {
				docker {
					label workerNode
					image "docker-io.dbc.dk/python3"
				}
			}
			when {
				branch "master"
			}
			steps {
				dir("deploy") {
					deploy_to_mesos("saturn-staging", "${env.DOCKER_TAG}", "${env.MARATHON_TOKEN}", {
						git(url: "gitlab@git-platform.dbc.dk:metascrum/deploy.git", credentialsId: "gitlab-meta")
					}, this)
				}
			}
		}
	}
}
