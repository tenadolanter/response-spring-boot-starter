module.exports = {
  branches: ['main'],
  tagFormat: 'v${version}',
  plugins: [
    ['@semantic-release/commit-analyzer', {
      preset: 'conventionalcommits',
      releaseRules: [
        {breaking: true, release: 'major'},
        {type: 'feat', release: 'minor'},
        {type: 'fix', release: 'patch'}
      ]
    }],
    ['@semantic-release/release-notes-generator', {
      preset: 'conventionalcommits'
    }],
    ['@semantic-release/exec', {
      prepareCmd: 'mvn -B org.codehaus.mojo:versions-maven-plugin:2.18.0:set -DnewVersion=${nextRelease.version} -DgenerateBackupPoms=false'
    }],
    ['@semantic-release/git', {
      assets: ['pom.xml'],
      message: 'chore(release): ${nextRelease.version} [skip ci]\n\n${nextRelease.notes}'
    }],
    '@semantic-release/github'
  ]
};
