compile:
	gradle clean assembleShadowDist
	path=$(shell find . -regex '.*/konsul-shadow-[0-9.]*.zip' | head -n 1) ;\
	unzip $$path -d build/distributions ;\
	ln -sfn $${path%".zip"}/bin/konsul

dockerBuild:
	path=$(shell find . -regex '.*/konsul-shadow-[0-9.]*.zip' | head -n 1) ;\
	docker build --build-arg path=$$path -t m9ffk4/konsul .

dockerPush:
	docker push m9ffk4/konsul

configGitHooks:
	git config core.hooksPath .githooks
