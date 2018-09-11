/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import React from "react";
import {Link} from "react-router-dom";

import constants from "../constants";

const Sidebar = () => (
    <nav id="sidebar">
        <img id="dbclogo" alt="dbclogo" width="120" height="42" src="data:image/gif;base64,R0lGODlheAAqALMPAGqvyIvB1CaMr5vK2r3c5u/2+TeVtRODqd7t887k7VimwkidvKzT4Hq4zgB6o////yH/C1hNUCBEYXRhWE1QPD94cGFja2V0IGJlZ2luPSLvu78iIGlkPSJXNU0wTXBDZWhpSHpyZVN6TlRjemtjOWQiPz4gPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iQWRvYmUgWE1QIENvcmUgNS4wLWMwNjEgNjQuMTQwOTQ5LCAyMDEwLzEyLzA3LTEwOjU3OjAxICAgICAgICAiPiA8cmRmOlJERiB4bWxuczpyZGY9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkvMDIvMjItcmRmLXN5bnRheC1ucyMiPiA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIiB4bWxuczp4bXBNTT0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL21tLyIgeG1sbnM6c3RSZWY9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZVJlZiMiIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG1wTU06T3JpZ2luYWxEb2N1bWVudElEPSJ4bXAuZGlkOjAzODAxMTc0MDcyMDY4MTE4M0VCOTVDMjI3ODQ3NzI1IiB4bXBNTTpEb2N1bWVudElEPSJ4bXAuZGlkOkJFRkNFODUyMkE3NjExRTM4RjQyODA2MzFFRDFFNTY1IiB4bXBNTTpJbnN0YW5jZUlEPSJ4bXAuaWlkOkJFRkNFODUxMkE3NjExRTM4RjQyODA2MzFFRDFFNTY1IiB4bXA6Q3JlYXRvclRvb2w9IkFkb2JlIFBob3Rvc2hvcCBDUzUuMSBNYWNpbnRvc2giPiA8eG1wTU06RGVyaXZlZEZyb20gc3RSZWY6aW5zdGFuY2VJRD0ieG1wLmlpZDowNDgwMTE3NDA3MjA2ODExODNFQjk1QzIyNzg0NzcyNSIgc3RSZWY6ZG9jdW1lbnRJRD0ieG1wLmRpZDowMzgwMTE3NDA3MjA2ODExODNFQjk1QzIyNzg0NzcyNSIvPiA8L3JkZjpEZXNjcmlwdGlvbj4gPC9yZGY6UkRGPiA8L3g6eG1wbWV0YT4gPD94cGFja2V0IGVuZD0iciI/PgH//v38+/r5+Pf29fTz8vHw7+7t7Ovq6ejn5uXk4+Lh4N/e3dzb2tnY19bV1NPS0dDPzs3My8rJyMfGxcTDwsHAv769vLu6ubi3trW0s7KxsK+urayrqqmop6alpKOioaCfnp2cm5qZmJeWlZSTkpGQj46NjIuKiYiHhoWEg4KBgH9+fXx7enl4d3Z1dHNycXBvbm1sa2ppaGdmZWRjYmFgX15dXFtaWVhXVlVUU1JRUE9OTUxLSklIR0ZFRENCQUA/Pj08Ozo5ODc2NTQzMjEwLy4tLCsqKSgnJiUkIyIhIB8eHRwbGhkYFxYVFBMSERAPDg0MCwoJCAcGBQQDAgEAACH5BAEAAA8ALAAAAAB4ACoAAAT/8MlJq7046827/2AojmRpnmiqBY5DqHAsW6w73/hZv3nv/xVAa0gsCgIBj6DIHAICCBKyeAgMgBNhc9tqFDZLblPx9SQUYkcVq00zDzxM2E0URDltt+FOa8UnAQZDR2UYeXREfxVziC0ChRcIgo1qCRc7FAQHWwwZbQArhw6WF3NXGAiBREmojA4GAaQSDA1GkBKYs2mnFp8cknVyQ7yeQwcZk46KFAVoQwp9Ng8Fm2qsDwNzfBS+HckO1xSmHNRDyw81LQu3F3mKuTWPFQWTDe1OHgVzAqXDHW3EJJRrYYCdMHUVcjk7l4DgvRagPAwg0mmRPw7pAqIjIosDASKQ/3ItOYZh0kMHEZXgs9hC44VaLSqKexZCkIJYFHI5yrCgxTYJ3TzAVGNhHIc5txoOkanDj4SdGHo6+PkgaAcGIFk6cBlkZc6sKUQSPXgyZQcEibRynaaqxQGqQw3AUBjzwsdXZUMQCegK0TmpKOc63ehAHrN6eUFU26oVkYGOFACHaypt4BoJ2YZQrepVZYvJfdNcLjpksolcD7CKMQ20c4c5oCkNgvwgGWspgyVo4lQMot6LE4xiGDC0UgXA9lSgBpQM1gar5Ciq/ZWMHwVnDqBhAQF9g9IWkIVrQAucMN7tH7prmDjklngN2LXrah+iAJQe6nufn9nSQzrrAkkHAtB7gd2Q3wUDOZAcf4x1QCCAEgC2QAiAyWWga+txRFp/HfxXAYHSdHDXZzgcOM9iFk7XAXYpCoSiQRckcwCMKJgY2V79cOgdEQsCQsQ6HAAGTg42PoCAkBPm2OB4jNBWWx1MVZDAN4Zd6BsqAWDn1mYPvFcBAcVlh8piBN0HiJbg4SebMU5KEBodb2WQAJl++SAKHQK06eaaxo33TRoCnCPDnWKMlsGbYgBA44eIlsYGIjcJuqEbVdy2AQEBHFLFWuh16umnoIYq6qiklmpqBxEAADs=" />
        <div id="sidebar-title">Saturnhøster</div>
        <ul>
            <li><Link to={constants.paths.httpConfigList}>
                Hentninger via HTTP
            </Link></li>
            <li><Link to={constants.paths.ftpConfigList}>
                Hentninger via FTP
            </Link></li>
        </ul>
    </nav>
)

export default Sidebar;
