import pysftp
import json
import os
from tempfile import NamedTemporaryFile
from logger import logger
from errormap import set_error, get_error
import paramiko
from pysftp import ConnectionException
from pysftpFactory import PySftpConnection
from internaldate import to_internal_date
from requestswrapper import requests_get, requests_post
import socks
import base64


class PasswordSyncer:
    password_change_enabled_sftp_host_names = json.loads(
        os.getenv("PASSWORD_CHANGE_ENABLED_SFTP_HOSTS") if not os.getenv(
            "PASSWORD_CHANGE_ENABLED_SFTP_HOSTS") == None else "[]")
    saturn_rest_endpoint = os.getenv("SATURN_REST_ENDPOINT") if not os.getenv(
        "SATURN_REST_ENDPOINT") == None else "http://no-server"
    sftp_configs_list = "configs/sftp/list"
    sftp_configs_add = "configs/sftp/add"
    password_repository_list = "passwordrepository/list/{}/{}"
    password_repository_add = "passwordrepository/add"

    def get_dates_and_passwords_from_from_sftp(self, harvester, cnopts):
        datesandpasswords = {}
        tmpMap = {}
        content_lines = self.sftp_get_password_file_as_lines_of_text(harvester, cnopts)
        if not content_lines == None:
            tmpMap = dict(line.split(" - ") for line in content_lines)
        for k, v in tmpMap.items():
            datesandpasswords[to_internal_date(k.strip())] = v.strip()
        return datesandpasswords

    def sftp_get_password_file_as_lines_of_text(self, harvester, cnopts):
        sftp = None
        content_lines = None
        try:
            logger.info("harvester config:{}".format(harvester["host"]))
            logger.info("sftp connection: '{}' at '{}':{} with pass: 'XXXX'".format(
                harvester["username"], harvester["host"],
                harvester["port"]
            ))
            sftp = PySftpConnection.getSftpConnection(harvester["host"], harvester["username"],
                                                      harvester["password"], harvester["port"])
            tmpfile = NamedTemporaryFile(mode='w+', prefix=harvester["username"], encoding='utf-8')
            sftp.get(harvester["username"], tmpfile.name)
            content_lines = tmpfile.read().splitlines()
            return content_lines

        except socks.GeneralProxyError as p:
            logger.info("   Proxy error '{}' for '{}'".format(p, harvester["host"]))
            set_error(harvester["name"], "   Proxy error '{}' for '{}'".format(p, harvester["host"]))

        except (ConnectionException, paramiko.ssh_exception.SSHException):
            set_error(harvester["name"],
                      "  Connection to {} unsuccesful. Check connection settings.".format(harvester["host"]))
            logger.error(get_error(harvester["name"]))

        except Exception as e:
            logger.info("   Error: {}".format(e))
            set_error(harvester["name"], "   Error: {}".format(e))

        finally:
            try:
                logger.info("   Connection to {} is about to be closed.".format(harvester["host"]))
                sftp.close()
                return content_lines
            except Exception:
                pass

    def get_persisted_dates_and_passwords_from_passwordstore(self, host, user):
        sub_url = self.password_repository_list.format(host, user)
        user_pass_dates = requests_get("{}/{}".format(self.saturn_rest_endpoint, sub_url))
        return user_pass_dates

    def entry_exists(self, entry, persisted_list):
        for p in persisted_list:
            if entry == p["activeFrom"]:
                return True
        return False

    def to_base64(self, pwd):
        return base64.b64encode(bytes(pwd, "utf-8")).decode("utf-8")

    def persist_to_passwordstore(self, entry):
        requests_post("{}/{}".format(self.saturn_rest_endpoint, self.password_repository_add), entry)
        logger.info(
            " Succesfully peristed password for {}@{} to be in effect after:{}".format(entry["username"], entry["host"],
                                                                                       entry["activeFrom"]))

    def persist_remote_dates_and_passwords_to_passwordstore(self):
        cnopts = pysftp.CnOpts()
        cnopts.hostkeys = None
        logger.info("SFTP host names to be checked:{}".format(self.password_change_enabled_sftp_host_names))
        logger.info("Saturn rest endpoint: {}".format(self.saturn_rest_endpoint))
        harvesters = requests_get("{}/{}".format(self.saturn_rest_endpoint, self.sftp_configs_list))
        for harvester in harvesters:
            if harvester["host"] in self.password_change_enabled_sftp_host_names:
                logger.info("Harvester: '{}'".format(harvester["name"]))
                pwd_map = self.get_dates_and_passwords_from_from_sftp(harvester, cnopts)
                persisted_list = self.get_persisted_dates_and_passwords_from_passwordstore(harvester["host"],
                                                                                           harvester["username"])

                if pwd_map != {}:
                    for pwd_entry_date, pw in pwd_map.items():
                        if not self.entry_exists(pwd_entry_date, persisted_list):
                            self.persist_to_passwordstore({"host": harvester["host"],
                                                           "username": harvester["username"],
                                                           "password": self.to_base64(pw),
                                                           "activeFrom": pwd_entry_date})
