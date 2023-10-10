/*!

JSZipUtils - A collection of cross-browser utilities to go along with JSZip.
<http://stuk.github.io/jszip-utils>

(c) 2014 Stuart Knightley, David Duponchel
Dual licenced under the MIT license or GPLv3. See https://raw.github.com/Stuk/jszip-utils/master/LICENSE.markdown.

*/
!function(a){"object"==typeof exports?module.exports=a():"function"==typeof define&&define.amd?define(a):"undefined"!=typeof window?window.JSZipUtils=a():"undefined"!=typeof global?global.JSZipUtils=a():"undefined"!=typeof self&&(self.JSZipUtils=a())}(function(){return function a(b,c,d){function e(g,h){if(!c[g]){if(!b[g]){var i="function"==typeof require&&require;if(!h&&i)return i(g,!0);if(f)return f(g,!0);throw new Error("Cannot find module '"+g+"'")}var j=c[g]={exports:{}};b[g][0].call(j.exports,function(a){var c=b[g][1][a];return e(c?c:a)},j,j.exports,a,b,c,d)}return c[g].exports}for(var f="function"==typeof require&&require,g=0;g<d.length;g++)e(d[g]);return e}({1:[function(a,b){"use strict";function c(){try{return new window.XMLHttpRequest}catch(a){}}function d(){try{return new window.ActiveXObject("Microsoft.XMLHTTP")}catch(a){}}var e={};e._getBinaryFromXHR=function(a){return a.response||a.responseText};var f=window.ActiveXObject?function(){return c()||d()}:c;e.getBinaryContent=function(a,b){try{var c=f();c.open("GET",a,!0),"responseType"in c&&(c.responseType="arraybuffer"),c.overrideMimeType&&c.overrideMimeType("text/plain; charset=x-user-defined"),c.onreadystatechange=function(){var d,f;if(4===c.readyState)if(200===c.status||0===c.status){d=null,f=null;try{d=e._getBinaryFromXHR(c)}catch(g){f=new Error(g)}b(f,d)}else b(new Error("Ajax error for "+a+" : "+this.status+" "+this.statusText),null)},c.send()}catch(d){b(new Error(d),null)}},b.exports=e},{}]},{},[1])(1)});
