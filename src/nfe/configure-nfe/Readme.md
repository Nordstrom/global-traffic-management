# developing

## setup virtualenv

```
pyenv virtualenv configure-nfe
pyenv activate configure-nfe
pip install -r requirements.txt
```

## publish

```
$ AWS_PROFILE=prod-nfe python publish/main.py 247808807723-nonprod-nfe-artifact nonprod v1.0.0 test
file: test/manifest.json
nfe/v1.0.0/manifest.json
```

## fetch

```
$ AWS_PROFILE=prod-nfe python fetch/main.py 247808807723-nonprod-nfe-artifact nonprod v1.0.0
manifest {u'files': [{u'dest': u'/home/admin/nfe/foo', u'name': u'local_name', u'encryption_key': u'foo'}]}
```

# building

## setup pex

```
pip install --user -r dist_requirements.txt
```

## build pex files

```
./build.sh
```
