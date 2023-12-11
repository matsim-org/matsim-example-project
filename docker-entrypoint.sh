#!/bin/sh
# docker-entrypoint.sh

set -e

export MATSIM_VERSION="$(cat resources/VERSION.txt)"

_print_header() {
    header_text="$(cat resources/BANNER.txt)

Environment:
$(env -0 | sort -z | tr '\0  ' '\n' | grep MATSIM | awk '{print "  ", $0}')"
    printf '%s\n' "$header_text"
}

_print_help_input() {
    help_text_input="
Error: The input directory $MATSIM_INPUT is empty.
Use the volume flag to bind a directory with the MATSim 
input files from the host to the conatiner:

    docker run \\
        -v <host/path/to/input>:/opt/matsim/data/input:ro \\
        -v <host/path/to/output>:/opt/matsim/data/output \\
        [...] \\
        maptic/matsim:latest

Exiting."
    printf '%s\n' "$help_text_input"
}

_print_help_output() {
    help_text_output="
Error: The output directory $MATSIM_OUTPUT is not
empty. Mount an empty folder or pass the environment
variable MATSIM_OUTPUT_OVERWRITE=true to the container:

    docker run \\
        -e MATSIM_OUTPUT_OVERWRITE=true \\
        [...] \\
        maptic/matsim:latest

Exiting."
    printf '%s\n' "$help_text_output"
}

_check_input_directory() {
    if [ -d "$MATSIM_INPUT" ] && files=$(ls -qAH -- "$MATSIM_INPUT") && [ -z "$files" ]; then
        _print_help_input
        exit 1
    fi
}

_check_output_directory() {
    if [ -d "$MATSIM_OUTPUT" ] && files=$(ls -qAH -- "$MATSIM_OUTPUT") && [ -z "$files" ]; then
        :
    else
        if [ $MATSIM_OUTPUT_OVERWRITE ]; then
            printf '\n%s\n' "Note: Overwriting the (not empty) output directory."
            sudo rm -rf $MATSIM_OUTPUT/*
        else
            _print_help_output
            exit 1
        fi
    fi
}

_print_header
_check_input_directory
_check_output_directory
echo "$COMMIT" > $MATSIM_OUTPUT/code-version.txt
printf '%s\n' ""
exec "java $INIRAM $MAXRAM -jar matsim.jar /opt/matsim/data/input/config.xml"
