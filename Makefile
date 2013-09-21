# Copyright 2013 Sneaky Squid LLC.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


#####################################
# Ensure ANDROID_HOME is set to path of Android SDK, and that API 18
# is installed.
#
# This Makefile will automatically download and install Gradle.
# Usage:
#    make       : Build Nova library
#    make clean : Clean up built files
# -Joe Walnes

ifndef ANDROID_HOME
$(error ANDROID_HOME not set)
endif

GRADLE_VERSION=1.7

all: nova-library.aar
.PHONY: all


#####################################
### Gradle installation

tools/gradle-$(GRADLE_VERSION).zip:
	$(info Downloading Gradle build tool. Be patient.)
	mkdir -p tools
	curl http://downloads.gradle.org/distributions/gradle-$(GRADLE_VERSION)-bin.zip > $@.download
	mv $@.download $@

tools/gradle-$(GRADLE_VERSION)/bin/gradle: tools/gradle-$(GRADLE_VERSION).zip
	$(info Unpacking Gradle)
	unzip -q $^ -d tools
	touch $@

gradle: tools/gradle-$(GRADLE_VERSION)/bin/gradle
	ln -sf $^ $@


#####################################
### Build library

library/build/libs/library.aar: gradle $(shell find library/src -type f) library/build.gradle
	./gradle build
	touch $@

nova-library.aar: library/build/libs/library.aar
	cp $^ $@


#####################################
### Build sample camera app

camera-app/build/apk/camera-app-release-unsigned.apk: gradle $(shell find camera-app/src -type f) camera-app/build.gradle
	./gradle build
	touch $@

nova-camera-app.apk: camera-app/build/apk/camera-app-release-unsigned.apk
	cp $^ $@


#####################################
### Cleanup

# Delete built files
clean:
	rm -rf .gradle library/build camera-app/build $(wildcard *.aar) $(wildcard *.apk)
.PHONY: clean

# Delete built files and Gradle installation
clobber: clean
	rm -rf tools gradle
.PHONY: clobber

