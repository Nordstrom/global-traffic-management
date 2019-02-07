pip wheel -w dist .
~/.local/bin/pex -vvvvvvvv configure-nfe==1.0.1 -r requirements.txt -f dist -e publish.main -o publish_config.pex
~/.local/bin/pex -vvvvvvvv configure-nfe==1.0.1 -r requirements.txt -f dist -e fetch.main -o fetch_config.pex
