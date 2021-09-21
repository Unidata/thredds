#!/usr/bin/env bash

set -euxo pipefail

main() {
  git fetch --prune origin "+refs/tags/*:refs/tags/*"
  OLD_VERSION=$(git tag -l '*.*.*' --sort=-version:refname | head -n 1)
  NEW_VERSION=$(bump2version --current-version $OLD_VERSION --list --tag --commit  --allow-dirty patch | grep -oP '^new_version=\K.*$')
  git push origin tag $NEW_VERSION

  exit 0
}

main "$@"
