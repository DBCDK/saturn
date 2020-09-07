
_error_map = {}

def set_error(k, v):
    _error_map[k] = v

def get_error(k):
    return _error_map[k]

def get_errormap():
    return _error_map

def errors():
    return _error_map != {}