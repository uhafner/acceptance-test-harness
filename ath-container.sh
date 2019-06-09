#!/usr/bin/env bash
# https://disconnected.systems/blog/another-bash-strict-mode/
set -euo pipefail
trap 's=$?; echo "$0: Error $s on line "$LINENO": $BASH_COMMAND"; exit $s' ERR

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"

uid=$(id -u)
gid=$(id -g)
gid=10000
tag="jenkins/ath"
java_version="${java_version:-8}"

mvn install -DskipTests

docker build --build-arg=uid="$uid" --build-arg=gid="$gid" "$DIR/src/main/resources/ath-container" -t "$tag"

run_opts="--rm --publish-all --user ath-user --workdir /home/ath-user/sources --shm-size 2g"
run_drive_mapping="-v /var/run/docker.sock:/var/run/docker.sock -v $(pwd):/home/ath-user/sources -v ${HOME}/.m2/repository:/home/ath-user/.m2/repository"
docker run $run_opts $run_drive_mapping $tag /bin/bash -c "set-java.sh $java_version; vnc.sh ; export DISPLAY=:42; run.sh firefox latest -Dmaven.test.failure.ignore=true -DforkCount=1 -B -Dtest=WarningsNextGenerationPluginTest"
