## Konsul 

Consul CLI for config synchronization Consul -> Git -> Consul

## Documentation

```cmd
$ konsul --help
Usage: Konsul [OPTIONS] COMMAND [ARGS]...

  Consul CLI for config synchronization consul -> git -> consul

Options:
  -h, --host TEXT     Consul host
  -p, --port INT      Consul port
  -pr, --prefix TEXT  Consul KV prefix
  -t, --token TEXT    Consul ACL token
  -d, --dry           Show the result of an operation without executing it
  --help              Show this message and exit

Commands:
  consulToGit  Sync Consul -> Git
  gitToConsul  Sync Git -> Consul
```

```cmd
$ konsul consulToGit --help
Usage: Konsul consulToGit [OPTIONS]

  Sync Consul -> Git

Options:
  -w, --workdir TEXT  The path to the directory where the configs will be
                      saved
  -h, --help          Show this message and exit
```

```cmd
$ konsul gitToConsul --help
Usage: Konsul gitToConsul [OPTIONS]

  Sync Git -> Consul

Options:
  -w, --workdir TEXT  The path to the directory from which consul kv configs
                      will be taken
  -h, --help          Show this message and exit
```


```cmd
$ konsul copyToConsul --help
Usage: Konsul copyToConsul [OPTIONS]

  Sync Git -> Consul

Options:
  -w, --workdir TEXT  The path to the directory from which consul kv configs
                      will be taken
  -с, --changes TEXT  Properties, that will be changed format "key:value,key1:value1"
  -h, --help          Show this message and exit
```

## Example

Создание дерева KV по пути `~/konsul` на основе Consul KV
```cmd
konsul \
-h=<host> \
-p=<port> \
-pr=<prefix> \
-t=<token> \
-d consulToGit -w="/home/konsul/config"
```

Пуш дерева KV в Consul из директории `~/konsul`

```cmd
konsul \
-h=<host> \
-p=<port> \
-pr=<prefix> \
-t=<token> \
-d gitToConsul -w="/home/konsul/config"
```

Пуш дерева KV в Consul из директории `~/konsul` с предварительной заменой значений в ключах <br>
`-c` - список изменений, которые будут применены ко всем KV в директории <br>
Пишется в формате `-c="<Ключ>:<Новое значение>,"<Ключ>:<Новое значение>"`

```cmd
konsul \
-h=<host> \
-p=<port> \
-pr=<prefix> \
-t=<token> \
-d copyToConsul 
-w="/home/konsul/config"
-c="sprning.data.somedata0:newData,prning.data.somedata1:newData1"
```