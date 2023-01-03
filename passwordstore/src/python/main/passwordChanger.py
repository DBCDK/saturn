import base64
import os
import json
from requestswrapper import requests_get, requests_post
from datetime import date
from internaldate import to_internal_date_from_python_date
from logger import logger

class PasswordChanger:
    GET_PASSWORD_CANDIDATE = "{}/{}/{}"
    saturn_rest_endpoint = os.getenv("SATURN_REST_ENDPOINT") if not os.getenv("SATURN_REST_ENDPOINT") == None else "http://no-server"
    password_change_enabled_sftp_host_names = json.loads(os.getenv("PASSWORD_CHANGE_ENABLED_SFTP_HOSTS") if not os.getenv("PASSWORD_CHANGE_ENABLED_SFTP_HOSTS") == None else "[]")
    sftp_configs_list = "configs/sftp/list"
    sftp_configs_add_or_modify = "configs/sftp/add"
    password_repository_get_date = "{}/{}".format(saturn_rest_endpoint, "passwordrepository/{}/{}/{}")

    def get_configs_for_host(self, hostname, allConfigs):
        configs = []
        for config in allConfigs:
            if config["host"] == hostname:
                configs.append(config)
        return configs

    def change_passwords(self):
        sftp_configs = requests_get("{}/{}".format(self.saturn_rest_endpoint, self.sftp_configs_list))
        logger.info("Hostnames: {}".format(self.password_change_enabled_sftp_host_names))
        for hostname in self.password_change_enabled_sftp_host_names:
            configs = self.get_configs_for_host(hostname, sftp_configs)
            for config in configs:
                    url = self.password_repository_get_date.format(hostname, config["username"],
                                                                   to_internal_date_from_python_date(date.today()))
                    passEntry = requests_get(url)
                    decodedPassword = base64.b64decode(passEntry["password"]).decode("utf-8")
                    if "activeFrom" in passEntry.keys() and not config["password"] == decodedPassword:
                        logger.info("Changedate for harvester '{}' at '{}@{}' is: {}".format(config["name"],
                                                                              passEntry["username"],
                                                                              passEntry["host"],
                                                                              passEntry["activeFrom"]))
                        logger.info("Changing host/user: {}/{} password".format(
                                    config["host"], config["username"]))
                        config["password"] = decodedPassword
                        requests_post("{}/{}".format(self.saturn_rest_endpoint, self.sftp_configs_add_or_modify), config)
                        logger.info("Config '{}' is now properly persisted.".format(config["name"]))
                    else:
                        logger.info("Unchanged '{}'".format(config["name"]))
