# RPi install and configuration details

## How to perform a fresh install
The Terror-Turret has only been tested on the Raspberry Pi 3B+ running 
Raspbian 9 Stretch. Many of the tools rely on the target system having the 
bash shell available and the root password disabled for sudo.
1. Download the install.sh script from the terror-turret/pi repo directory.
2. [Optional] Edit the install.sh file and change the __DEFAULT_PROJECT_NAME__
and/or the __DEFAULT_PASSWORD__.
3. Run the install script:
```$ bash install.sh```
There are two optional parameters you can use with the install script. `-d` will
skip input prompts and select the default values. `-q` will set _-d_ and redirect
all output to a log file `/tmp/turret_install.log`
4. Unless skipped the last step is to respond to the prompts for a project name 
and a turret password.

After the system reboots the turret should be ready for use. 

**Please note:** The installer does not fail gracefully. If there is a problem, 
the script will exit at that point, but will not roll-back previous actions.
After you correct the issue, you should be able to run the script again.
Otherwise you can use the install script as a guide on what to do to
finish the install.

## Configuring the Turret software
There are a number of configuration options available. The two primary config
files (post-install) are `/usr/share/${project_name}/turretManagerConfig.py`
and `/usr/share/${project_name}/uv4l-config.conf`. Where ${project_name}
defaults to __terror-turret__. These files can be edited by hand or using the
`turret` commandline tool.

### turretManagerConfig.py
This file controls the Python Websocket Server startup parameters. Please
examine the file at `/usr/share/${project_name}/turretManagerConfig.py` to see
the specific options.

### uv4l-config.conf
This file sets the options for the video and audio streaming server. 


## How to enable SSL for your turret
The basic steps are:
1. Get SSL/TLS certificate
2. Copy certificate to the turret server
3. Configure the turret server

_This can be done with self-signed certs. But you can ask Google for help with that._
There are many ways to get a valid SSL/TLS certificate. This will only describe using [Letsencrypt](https://letsencrypt.org/) and [Certbot](https://certbot.eff.org/) on an FQDN (fully qualified domain name).
This is how our SSL is setup.

## Get a SSL/TLS certificate

### Domain Registration
Register a domain name of your choosing. For this method you'll have to add A and TXT records, so make sure your domain is pointed to a DNS manager with those features. We got our domain from https://freenom.tk (it was free) which also has acceptable DNS management.

### Generate Certificate
Certbot run on Linux, which you should have already installed on your Raspberry Pi. You can run it from any linux system and copy the generated certificates to your turret server afterwards. Follow the installation instructions [here](https://certbot.eff.org/docs/install.html). We use the [manual method](https://certbot.eff.org/docs/using.html#manual) for domain validation and do not use a webserver with Certbot, so we did not install the `python-cerbot-apache` or `python-certbot-nginx` packages. _The main drawback to using the manual method is you have to manually renew your certificate every 3 months. It only takes a few minutes, but it's another thing you have to do._

#### Verify Domain Ownership
At this point we had the choice of using either `http` or `dns` challenge. For `http` you will need to setup a static webpage before proceeding. With `dns` you have to do it after you finish with Certbot. We used `dns` method since it's more straightforward. During the execution of certbot you'll have to add TXT records for each subdomain you want on the certificate. Keep the page to your DNS manager open.
_Letsencrypt now supports wildcard domain names. Google it if you're interested._ 
Here's an example of how to register multiple subdomains:
```./certbot run certonly --manual --preferred-challenges dns -d terror-turret.tk -d www.terror-turret.tk -d kittens.terror-turret.tk -d```
If you want to verify a TXT record has propegated (so that certbot will see it) you can use the webpage [mxtoolbox.com/SuperTool.aspx?action=txt] or from a linux commandline `dig -t TXT _acme-challenge.<subdomain>.<domain>` or `host -t TXT _acme-challenge.<subdomain>.<domain>`.

#### Public Facing Webpage
Letsencrypt expects one of the subdomains used to resolve to a public webpage. If it doesn't find one, it will revoke your certificate. We created a GitLabs Pages site (Github also supports this method) and uploaded the generated certificates (note for the __gitlab certificate__ box just copy the entire contents of the fullchain.pem file). Gitlab requires a one-time verification of DNS ownership, basically another TXT record. _You will have to update the certificates every 3 months when you renew your certificate._

## Copy Certificate to Turret Server
If you ran certbot on your turret server you can skip this step. Certbot displayed the path to your cert files when it finished running. Usually they are in the `/etc/letsencrypt/live/<domain>/` folder as symlinks. To copy them though, they are in the `/etc/letsencrypt/archive/<domain>/` folder. The folder is owned by root, so you'll need elevated privledges to get the files. Every time you renew your certs the new files are stored in this folder with a number appended to the filename. The most recent certificate will have the highest number. 
To copy the cert files to the turret server you'll need to copy the `cert#.pem`, `fullchain#.pem`, and `privkey#.pem` files. Please note, you need to keep the privkey.pem file a secret. If need you can zip the pem files or even encrypt them for transport. Or just use something like SCP or SFTP. On the turret server save the files in the `/etc/uv4l/` folder (_if you ran certbot on the server, leave them in letsencrypt folder_).
