/**
 * Created by fide on 31.03.17.
 */

import url from "url"

const homepage = process.env.PUBLIC_URL
    ? process.env.PUBLIC_URL
    : "";

const base_url = homepage ? url.parse(homepage).pathname : "";
export const BASE_URL_PATH = base_url ? base_url : "/";
export const SINGLE_WORD_URL_PATH = `${base_url}/single-word`;
export const ALL_WORD_URL_PATH = `${base_url}/all-named-entities`;
