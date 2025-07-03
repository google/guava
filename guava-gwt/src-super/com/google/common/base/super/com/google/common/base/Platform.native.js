/**
 * @param {?string} str
 * @return {boolean} Whether the given string is null or is the empty string.
 * @public
 */
Platform.stringIsNullOrEmpty = function(str) {
  return !str;
};


/**
 * @param {?string} str
 * @return {string} Original str, if it is non-null. Otherwise empty string.
 */
Platform.nullToEmpty = function(str) {
  return str || "";
};


/**
 * @param {?string} str
 * @return {string} Original str, if it is non-empty. Otherwise null;
 */
Platform.emptyToNull = function(str) {
  return str || null;
};
