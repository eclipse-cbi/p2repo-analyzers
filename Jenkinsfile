pipeline {
  agent {
      label 'centos-8'
  }

  environment {
    MAVEN_OPTS='-Xmx1024m -Xms256m -XshowSettings:vm -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn'
    MAVEN_CONFIG = '-B -C -U -e'
    PUBLISH_LOCATION = 'cbi/updates/p2-analyzers'
  }

  tools {
    maven 'apache-maven-latest'
    jdk 'openjdk-latest'
  }

  options {
    buildDiscarder(logRotator(numToKeepStr: '10'))
    disableConcurrentBuilds()
  }

  parameters {
    choice(
      name: 'BUILD_TYPE',
      choices: ['nightly', 'milestone', 'release'],
      description: '''
        Choose the type of build.
        Note that a release build will not promote the build, but rather will promote the most recent milestone build.
        '''
    )

    booleanParam(
      name: 'PROMOTE',
      defaultValue: false,
      description: 'Whether to promote the build to the download server.'
    )
  }


  stages {

    stage('Display Parameters') {
      steps {
        echo "BUILD_TYPE=${params.BUILD_TYPE}"
        echo "PROMOTE=${params.PROMOTE}"
        script {
          env.PROMOTE = params.PROMOTE
          env.BUILD_TYPE = params.BUILD_TYPE
        }
      }
    }

    stage('Git Checkout') {
      when {
        expression {
          // This stage is useful for testing changes to the pipeline.
          // The changes can be pasted into a pipeline job to test them before committing them.
          false
        }
      }

      steps {
        script {
          def gitVariables = checkout(
            poll: false,
            scm: [
              $class: 'GitSCM',
              branches: [[name: '*/main']],
              doGenerateSubmoduleConfigurations: false,
              submoduleCfg: [],
              userRemoteConfigs: [[url: 'https://github.com/eclipse-cbi/p2repo-analyzers.git']]
            ]
          )

          echo "$gitVariables"
          env.GIT_COMMIT = gitVariables.GIT_COMMIT
          env.MAVEN_PROFILES = "-Peclipse-sign -Ppromote"
        }
      }
    }

    stage('Check for Automatic Deployment') {
      when {
        branch 'main'
      }
      steps {
        echo "Automatically deploy when building the main branch."
        script {
          env.PROMOTE = true
          env.MAVEN_PROFILES = "-Peclipse-sign -Ppromote"
        }
      }
    }

    stage('Build Tools and Products') {
      steps {
        sshagent(['projects-storage.eclipse.org-bot-ssh']) {
          dir('releng/org.eclipse.cbi.p2repo.analyzers.parent') {
            sh '''
              pwd
              if [[ $PROMOTE == false ]]; then
                promotion_argument="-Dorg.eclipse.justj.p2.manager.args=-remote localhost:${PWD}/promotion/promo-repo"
              else
                promotion_argument="-Dorg.eclipse.justj.p2.manager.args.default=true"
              fi
              mvn \
                --no-transfer-progress \
                "$promotion_argument" \
                -Dorg.eclipse.storage.user=genie.cbi \
                -Dorg.eclipse.justj.p2.manager.build.url=$JOB_URL \
                -Dorg.eclipse.download.location.relative=$PUBLISH_LOCATION \
                -Dorg.eclipse.justj.p2.manager.relative= \
                -Dbuild.type=$BUILD_TYPE \
                -Dgit.commit=$GIT_COMMIT \
                -Dbuild.id=$BUILD_NUMBER \
                -DskipTests=false \
                $MAVEN_PROFILES \
                clean \
                verify
              '''
          }
        }
      }
      post {
        always {
            junit '**/target/surefire-reports/TEST-*.xml'
            archiveArtifacts artifacts: 'releng/org.eclipse.cbi.p2repo.analyzers.repository/target/**, releng/org.eclipse.cbi.p2repo.analyzers.product/target/**, releng/org.eclipse.cbi.p2repo.analyzers.parent/promotion/**'
        }
      }
    }
  }

  post {
    failure {
      mail to: 'ed.merks@gmail.com',
      subject: "[CBI p2 Analyzer] Build Failure ${currentBuild.fullDisplayName}",
      mimeType: 'text/html',
      body: "Project: ${env.JOB_NAME}<br/>Build Number: ${env.BUILD_NUMBER}<br/>Build URL: ${env.BUILD_URL}<br/>Console: ${env.BUILD_URL}/console"
    }

    fixed {
      mail to: 'ed.merks@gmail.com',
      subject: "[CBI p2 Analyzer] Back to normal ${currentBuild.fullDisplayName}",
      mimeType: 'text/html',
      body: "Project: ${env.JOB_NAME}<br/>Build Number: ${env.BUILD_NUMBER}<br/>Build URL: ${env.BUILD_URL}<br/>Console: ${env.BUILD_URL}/console"
    }

    cleanup {
      deleteDir()
    }
  }
}
