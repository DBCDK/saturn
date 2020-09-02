import datetime

_internal_datetime_format = "%Y-%m-%dT%H:%M:%S"

def to_internal_date(stringDate):
    mdy = tuple(map(int, stringDate.split('/')))
    return datetime.date(year=mdy[2], month=mdy[0], day=mdy[1]).strftime(_internal_datetime_format)
