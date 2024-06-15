#!/bin/sh

export MSYS=winsymlinks:nativestrict

mod_config_name="trimeffects.mixins.json"
mod_source_path="dev/foxgirl/trimeffects"

cd ..

for loader in fabric neoforge forge; do

    if [ -d $loader ]; then

        echo "Creating links for loader $loader..."

        set -o xtrace

        cd $loader/src/main

        cd resources
        rm $mod_config_name
        ln -s ../../../../mixins/$mod_config_name $mod_config_name
        cd ..

        cd java/$mod_source_path
        rm -r mixin
        ln -s ../../../../../../../mixins/mixin mixin
        cd ../../../..

        cd ../../..

        set +o xtrace

    fi

done
