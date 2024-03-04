#!/usr/bin/env python3

#from passwordSyncer import PasswordSyncer
#from logger import logger
#from errormap import errors, get_errormap
#from passwordChanger import PasswordChanger
import base64

def main():
    print("BASE64:"+ base64.b64encode(bytes("gNwXt9x=eADM", "utf-8")).decode("utf-8"))
    changer = PasswordChanger()
    changer.change_passwords()
    syncer = PasswordSyncer()
    syncer.persist_remote_dates_and_passwords_to_passwordstore()
    changer.change_passwords()T
    if errors():
        logger.error("Errors found: {}".format(get_errormap()))
        exit(1)


if __name__ == '__main__':
    main()
