compile:
	gradle clean assembleShadowDist
	path=$(shell find . -regex '.*/konsul-shadow-[0-9.]*.zip' | head -n 1) ;\
	unzip $$path -d build/distributions ;\
	ln -sfn $${path%".zip"}/bin/konsul
