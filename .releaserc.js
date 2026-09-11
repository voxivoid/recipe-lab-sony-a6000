/**
 * The version is decided from commit messages; AndroidManifest.xml is written by
 * tools/bump-version.sh, which also computes versionCode from the formula.
 *
 * There is no semantic-release plugin for a bare AndroidManifest.xml (Flutter projects
 * use semantic-release-pub for this), so @semantic-release/exec drives our own script.
 */
export default {
  branches: [{ name: 'main' }],

  plugins: [
    [
      '@semantic-release/commit-analyzer',
      {
        preset: 'conventionalcommits',
        releaseRules: [{ type: 'refactor', release: 'patch' }],
      },
    ],
    [
      '@semantic-release/release-notes-generator',
      {
        preset: 'conventionalcommits',
        presetConfig: {
          types: [
            { type: 'feat', section: 'New' },
            { type: 'fix', section: 'Fixed' },
            { type: 'perf', section: 'Performance' },
            { type: 'refactor', section: 'Refactor' },
            { type: 'docs', section: 'Documentation' },
            { type: 'build', section: 'Build' },
            { type: 'ci', hidden: true },
            { type: 'chore', hidden: true },
            { type: 'test', hidden: true },
          ],
        },
      },
    ],
    [
      '@semantic-release/exec',
      {
        // Writes versionName + versionCode, then builds the APK that gets published.
        prepareCmd:
          './tools/bump-version.sh ${nextRelease.version} && RELEASE=1 RELEASE_TAG=v${nextRelease.version} ./build.sh',
      },
    ],
    [
      '@semantic-release/github',
      {
        assets: [
          { path: 'RecipeLab.apk', label: 'RecipeLab-${nextRelease.version}.apk' },
          { path: 'RecipeLab.apk.sha256', label: 'RecipeLab-${nextRelease.version}.apk.sha256' },
        ],
        successComment: false,
        failComment: false,
      },
    ],
    [
      '@semantic-release/git',
      {
        assets: ['AndroidManifest.xml'],
        message: 'chore(release): ${nextRelease.version} [skip ci]',
      },
    ],
  ],
};
