#!/usr/bin/env python3

from passwordSyncer import PasswordSyncer
from passwordSyncer import logger

def main():
    psc = PasswordSyncer();
    psc.persist_remote_dates_and_passwords_to_passwordstore()
    if psc._error_map:
        logger.error("Errors found: {}".format(psc.get_errormap()))
        exit(1)

if __name__ == '__main__':
    main()