#!/usr/bin/env python3

from passwordSyncer import PasswordSyncer
from logger import logger
from errormap import errors, get_errormap
from passwordChanger import PasswordChanger

def main():
    changer = PasswordChanger()
    changer.change_passwords()
    syncer = PasswordSyncer()
    syncer.persist_remote_dates_and_passwords_to_passwordstore()
    changer.change_passwords()
    if errors():
        logger.error("Errors found: {}".format(get_errormap()))
        exit(1)

if __name__ == '__main__':
    main()
