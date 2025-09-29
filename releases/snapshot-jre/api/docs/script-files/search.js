/*
 * Copyright (c) 2015, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */
"use strict";
const messages = {
    enterTerm: "Enter a search term",
    noResult: "No results found",
    oneResult: "Found one result",
    manyResults: "Found {0} results",
    loading: "Loading search index...",
    searching: "Searching...",
    redirecting: "Redirecting to first result...",
}
const categories = {
    modules: "Modules",
    packages: "Packages",
    types: "Types",
    members: "Members",
    searchTags: "Search Tags"
};
// Localized element descriptors must match values in enum IndexItem.Kind.
const itemDesc = [
    // Members
    ["Enum constant in {0}"],
    ["Variable in {0}"],
    ["Static variable in {0}"],
    ["Constructor for {0}"],
    ["Element in {0}"],
    ["Method in {0}"],
    ["Static method in {0}"],
    ["Record component of {0}"],
    // Types in upper and lower case
    ["Annotation Type", "annotation type"],
    ["Enum",           "enum"],
    ["Interface",      "interface"],
    ["Record Class",    "record class"],
    ["Class",          "class"],
    ["Exception Class", "exception class"],
    // Tags
    ["Search tag in {0}"],
    ["System property in {0}"],
    ["Section in {0}"],
    ["External specification in {0}"],
    // Other
    ["Summary Page"],
];
const mbrDesc = "Member";
const clsDesc = "Class"
const pkgDesc = "Package";
const mdlDesc = "Module";
const pkgDescLower = "package";
const mdlDescLower = "module";
const tagDesc = "Search Tag";
const inDesc = "{0} in {1}";
const descDesc = "Description";
const linkLabel = "Go to search page";
const NO_MATCH = {};
const MAX_RESULTS = 300;
const UNICODE_LETTER = 0;
const UNICODE_DIGIT = 1;
const UNICODE_OTHER = 2;
function checkUnnamed(name, separator) {
    return name === "<Unnamed>" || !name ? "" : name + separator;
}
function escapeHtml(str) {
    return str.replace(/</g, "&lt;").replace(/>/g, "&gt;");
}
function getHighlightedText(str, boundaries, from, to) {
    var start = from;
    var text = "";
    for (var i = 0; i < boundaries.length; i += 2) {
        var b0 = boundaries[i];
        var b1 = boundaries[i + 1];
        if (b0 >= to || b1 <= from) {
            continue;
        }
        text += escapeHtml(str.slice(start, Math.max(start, b0)));
        text += "<span class='result-highlight'>";
        text += escapeHtml(str.slice(Math.max(start, b0), Math.min(to, b1)));
        text += "</span>";
        start = Math.min(to, b1);
    }
    text += escapeHtml(str.slice(start, to));
    return text;
}
function getURLPrefix(item, category) {
    var urlPrefix = "";
    var slash = "/";
    if (category === "modules") {
        return item.l + slash;
    } else if (category === "packages" && item.m) {
        return item.m + slash;
    } else if (category === "types" || category === "members") {
        if (item.m) {
            urlPrefix = item.m + slash;
        } else {
            $.each(packageSearchIndex, function(index, it) {
                if (it.m && item.p === it.l) {
                    urlPrefix = it.m + slash;
                    item.m = it.m;
                    return false;
                }
            });
        }
    }
    return urlPrefix;
}
function getURL(item, category) {
    if (item.url) {
        return item.url;
    }
    var url = getURLPrefix(item, category);
    if (category === "modules") {
        url += "module-summary.html";
    } else if (category === "packages") {
        if (item.u) {
            url = item.u;
        } else {
            url += item.l.replace(/\./g, '/') + "/package-summary.html";
        }
    } else if (category === "types") {
        if (item.u) {
            url = item.u;
        } else {
            url += checkUnnamed(item.p, "/").replace(/\./g, '/') + item.l + ".html";
        }
    } else if (category === "members") {
        url += checkUnnamed(item.p, "/").replace(/\./g, '/') + item.c + ".html" + "#";
        if (item.u) {
            url += item.u;
        } else {
            url += item.l;
        }
    } else if (category === "searchTags") {
        url += item.u;
    }
    item.url = url;
    return url;
}
function createMatcher(term, camelCase) {
    if (camelCase && !isUpperCase(term)) {
        return null;  // no need for camel-case matcher for lower case query
    }
    var pattern = "";
    var upperCase = [];
    term.trim().split(/\s+/).forEach(function(w, index, array) {
        var tokens = w.split(/(?=[\p{Lu},.()<>?[\/])/u);
        for (var i = 0; i < tokens.length; i++) {
            var s = tokens[i];
            // ',' and '?' are the only delimiters commonly followed by space in java signatures
            pattern += "(" + escapeUnicodeRegex(s).replace(/[,?]/g, "$&\\s*?") + ")";
            upperCase.push(false);
            if (i === tokens.length - 1 && index < array.length - 1) {
                // space in query string matches all delimiters
                pattern += "(.*?)";
                upperCase.push(isUpperCase(s[0]));
            } else {
                if (!camelCase && isUpperCase(s) && s.length === 1) {
                    pattern += "()";
                } else {
                    pattern += "([\\p{L}\\p{Nd}\\p{Sc}<>?[\\]]*?)";
                }
                upperCase.push(isUpperCase(s[0]));
            }
        }
    });
    var re = new RegExp(pattern, camelCase ? "gu" : "gui");
    re.upperCase = upperCase;
    return re;
}
// Unicode regular expressions do not allow certain characters to be escaped
function escapeUnicodeRegex(pattern) {
    return pattern.replace(/[\[\]{}()*+?.\\^$|\s]/g, '\\$&');
}
function findMatch(matcher, input, startOfName, endOfName, prefixLength) {
    var from = startOfName;
    matcher.lastIndex = from;
    var match = matcher.exec(input);
    // Expand search area until we get a valid result or reach the beginning of the string
    while (!match || match.index + match[0].length < startOfName || endOfName < match.index) {
        if (from === 0) {
            return NO_MATCH;
        }
        from = input.lastIndexOf(".", from - 2) + 1;
        matcher.lastIndex = from;
        match = matcher.exec(input);
    }
    var boundaries = [];
    var matchEnd = match.index + match[0].length;
    var score = 5;
    var start = match.index;
    var prevEnd = -1;
    for (var i = 1; i < match.length; i += 2) {
        var charType = getCharType(input[start]);
        // capturing groups come in pairs, match and non-match
        boundaries.push(start, start + match[i].length);
        var prevChar = input[start - 1] || "";
        var nextChar = input[start + 1] || "";
        // make sure group is anchored on a word boundary
        if (start !== 0 && start !== startOfName) {
            if (charType === UNICODE_DIGIT && getCharType(prevChar) === UNICODE_DIGIT) {
                return NO_MATCH; // Numeric token must match at first digit
            } else if (charType === UNICODE_LETTER && getCharType(prevChar) === UNICODE_LETTER) {
                if (!isUpperCase(input[start]) || (!isLowerCase(prevChar) && !isLowerCase(nextChar))) {
                    // Not returning NO_MATCH below is to enable upper-case query strings
                    if (!matcher.upperCase[i] || start !== prevEnd) {
                        return NO_MATCH;
                    } else if (!isUpperCase(input[start])) {
                        score -= 1.0;
                    }
                }
            }
        }
        prevEnd = start + match[i].length;
        start += match[i].length + match[i + 1].length;

        // Lower score for unmatched parts between matches
        if (match[i + 1]) {
            score -= rateDistance(match[i + 1]);
        }
    }

    // Lower score for unmatched leading part of name
    if (startOfName < match.index) {
        score -= rateDistance(input.substring(startOfName, match.index));
    }
    // Favor child or parent variety depending on whether parent is included in search
    var matchIncludesContaining = match.index < startOfName;
    // Lower score for unmatched trailing part of name, but exclude member listings
    if (matchEnd < endOfName && input[matchEnd - 1] !== ".") {
        let factor = matchIncludesContaining ? 0.1 : 0.8;
        score -= rateDistance(input.substring(matchEnd, endOfName)) * factor;
    }
    // Lower score for unmatched prefix in member class name
    if (prefixLength < match.index && prefixLength < startOfName) {
        let factor = matchIncludesContaining ? 0.8 : 0.4;
        score -= rateDistance(input.substring(prefixLength, Math.min(match.index, startOfName))) * factor;
    }
    // Rank qualified names by package name
    if (prefixLength > 0) {
        score -= rateDistance(input.substring(0, prefixLength)) * 0.2;
    }
    // Reduce score of constructors in member listings
    if (matchEnd === prefixLength) {
        score -= 0.1;
    }

    return score > 0 ? {
        input: input,
        score: score,
        boundaries: boundaries
    } : NO_MATCH;
}
function isLetter(s) {
    return /\p{L}/u.test(s);
}
function isUpperCase(s) {
    return /\p{Lu}/u.test(s);
}
function isLowerCase(s) {
    return /\p{Ll}/u.test(s);
}
function isDigit(s) {
    return /\p{Nd}/u.test(s);
}
function getCharType(s) {
    if (isLetter(s)) {
        return UNICODE_LETTER;
    } else if (isDigit(s)) {
        return UNICODE_DIGIT;
    } else {
        return UNICODE_OTHER;
    }
}
function rateDistance(str) {
    // Rate distance of string by counting word boundaries and camel-case tokens
    return !str ? 0
        : (str.split(/\b|(?<=[\p{Ll}_])\p{Lu}/u).length * 0.1
            + (isUpperCase(str[0]) ? 0.08 : 0));
}
function doSearch(request, response) {
    var term = request.term.trim();
    var maxResults = request.maxResults || MAX_RESULTS;
    var module = checkUnnamed(request.module, "/");
    var matcher = {
        plainMatcher: createMatcher(term, false),
        camelCaseMatcher: createMatcher(term, true)
    }
    var indexLoaded = indexFilesLoaded();

    function getPrefix(item, category) {
        switch (category) {
            case "packages":
                return checkUnnamed(item.m, "/");
            case "types":
            case "members":
                return checkUnnamed(item.p, ".");
            default:
                return "";
        }
    }
    function getClassPrefix(item, category) {
        if (category === "members" && (!item.k || (item.k < 8 && item.k !== "3"))) {
            return item.c + ".";
        }
        return "";
    }
    function searchIndex(indexArray, category) {
        var matches = [];
        if (!indexArray) {
            if (!indexLoaded) {
                matches.push({ l: messages.loading, category: category });
            }
            return matches;
        }
        $.each(indexArray, function (i, item) {
            if (module) {
                var modulePrefix = getURLPrefix(item, category) || item.u;
                if (modulePrefix.indexOf("/") > -1 && !modulePrefix.startsWith(module)) {
                    return;
                }
            }
            var prefix = getPrefix(item, category);
            var classPrefix = getClassPrefix(item, category);
            var simpleName = classPrefix + item.l;
            if (item.d) {
                simpleName += " - " + item.d;
            }
            var qualName = prefix + simpleName;
            var startOfName = classPrefix.length + prefix.length;
            var endOfName = category === "members" && qualName.indexOf("(", startOfName) > -1
                ? qualName.indexOf("(", startOfName) : qualName.length;
            var m = findMatch(matcher.plainMatcher, qualName, startOfName, endOfName, prefix.length);
            if (m === NO_MATCH && matcher.camelCaseMatcher) {
                m = findMatch(matcher.camelCaseMatcher, qualName, startOfName, endOfName, prefix.length);
            }
            if (m !== NO_MATCH) {
                m.indexItem = item;
                m.name = simpleName;
                m.category = category;
                if (m.boundaries[0] < prefix.length) {
                    m.name = qualName;
                } else {
                    m.boundaries = m.boundaries.map(function(b) {
                        return b - prefix.length;
                    });
                }
                // m.name = m.name + " " + m.score.toFixed(3);
                matches.push(m);
            }
            return true;
        });
        return matches.sort(function(e1, e2) {
            return e2.score - e1.score
                || (category !== "members"
                    ? e1.name.localeCompare(e2.name) : 0);
        }).slice(0, maxResults);
    }

    var result = searchIndex(moduleSearchIndex, "modules")
         .concat(searchIndex(packageSearchIndex, "packages"))
         .concat(searchIndex(typeSearchIndex, "types"))
         .concat(searchIndex(memberSearchIndex, "members"))
         .concat(searchIndex(tagSearchIndex, "searchTags"));

    if (!indexLoaded) {
        updateSearchResults = function() {
            doSearch(request, response);
        }
    } else {
        updateSearchResults = function() {};
    }
    response(result);
}
// JQuery search menu implementation
$.widget("custom.catcomplete", $.ui.autocomplete, {
    _create: function() {
        this._super();
        this.widget().menu("option", "items", "> .result-item");
        // workaround for search result scrolling
        this.menu._scrollIntoView = function _scrollIntoView( item ) {
            var borderTop, paddingTop, offset, scroll, elementHeight, itemHeight;
            if ( this._hasScroll() ) {
                borderTop = parseFloat( $.css( this.activeMenu[ 0 ], "borderTopWidth" ) ) || 0;
                paddingTop = parseFloat( $.css( this.activeMenu[ 0 ], "paddingTop" ) ) || 0;
                offset = item.offset().top - this.activeMenu.offset().top - borderTop - paddingTop;
                scroll = this.activeMenu.scrollTop();
                elementHeight = this.activeMenu.height() - 26;
                itemHeight = item.outerHeight();

                if ( offset < 0 ) {
                    this.activeMenu.scrollTop( scroll + offset );
                } else if ( offset + itemHeight > elementHeight ) {
                    this.activeMenu.scrollTop( scroll + offset - elementHeight + itemHeight );
                }
            }
        };
    },
    _renderMenu: function(ul, items) {
        var currentCategory = "";
        var widget = this;
        widget.menu.bindings = $();
        $.each(items, function(index, item) {
            if (item.category && item.category !== currentCategory) {
                ul.append("<li class='ui-autocomplete-category'>" + categories[item.category] + "</li>");
                currentCategory = item.category;
            }
            var li = widget._renderItemData(ul, item);
            if (item.category) {
                li.attr("aria-label", categories[item.category] + " : " + item.l);
            } else {
                li.attr("aria-label", item.l);
            }
            li.attr("class", "result-item");
        });
        ul.append("<li class='ui-static-link'><div><a href='" + pathtoroot + "search.html?q="
            + encodeURI(widget.term) + "'>" + linkLabel + "</a></div></li>");
    },
    _renderItem: function(ul, item) {
        var label = getResultLabel(item);
        var resultDesc = getResultDescription(item);
        return $("<li/>")
            .append($("<div/>")
                .append($("<span/>").addClass("search-result-label").html(label))
                .append($("<span/>").addClass("search-result-desc").html(resultDesc)))
            .appendTo(ul);
    },
    _resizeMenu: function () {
        var ul = this.menu.element;
        var missing = 0;
        ul.children().each((i, e) => {
            if (e.hasChildNodes() && e.firstChild.hasChildNodes()) {
                var label = e.firstChild.firstChild;
                missing = Math.max(missing, label.scrollWidth - label.clientWidth);
            }
        });
        ul.outerWidth( Math.max(
            ul.width("").outerWidth() + missing + 40,
            this.element.outerWidth()
        ));
    }
});
function getResultLabel(item) {
    if (item.l) {
        return item.l;
    }
    return getHighlightedText(item.name, item.boundaries, 0, item.name.length);
}
function getResultDescription(item) {
    if (!item.indexItem) {
        return "";
    }
    var kind;
    switch (item.category) {
        case "members":
            var typeName = checkUnnamed(item.indexItem.p, ".") + item.indexItem.c;
            var typeDesc = getEnclosingTypeDesc(item.indexItem);
            kind = itemDesc[item.indexItem.k || 5][0];
            return kind.replace("{0}", typeDesc + " " + typeName);
        case "types":
            var pkgName = checkUnnamed(item.indexItem.p, "");
            kind = itemDesc[item.indexItem.k || 12][0];
            if (!pkgName) {
                // Handle "All Classes" summary page and unnamed package
                return item.indexItem.k === "18" ? kind : kind + " " + item.indexItem.l;
            }
            return getEnclosingDescription(kind, pkgDescLower, pkgName);
        case "packages":
            if (item.indexItem.k === "18") {
                return itemDesc[item.indexItem.k][0]; // "All Packages" summary page
            } else if (!item.indexItem.m) {
                return pkgDesc + " " + item.indexItem.l;
            }
            var mdlName = item.indexItem.m;
            return getEnclosingDescription(pkgDesc, mdlDescLower, mdlName);
        case "modules":
            return mdlDesc + " " + item.indexItem.l;
        case "searchTags":
            if (item.indexItem) {
                var holder = item.indexItem.h;
                kind = itemDesc[item.indexItem.k || 14][0];
                return holder ? kind.replace("{0}", holder) : kind;
            }
    }
    return "";
}
function getEnclosingDescription(elem, desc, label) {
    return inDesc.replace("{0}", elem).replace("{1}", desc + " " + label);
}
function getEnclosingTypeDesc(item) {
    if (!item.typeDesc) {
        $.each(typeSearchIndex, function(index, it) {
            if (it.l === item.c && it.p === item.p && it.m === item.m) {
                item.typeDesc = itemDesc[it.k || 12][1];
                return false;
            }
        });
    }
    return item.typeDesc || "";
}
$(function() {
    var search = $("#search-input");
    var reset = $("#reset-search");
    search.catcomplete({
        minLength: 1,
        delay: 200,
        source: function(request, response) {
            if (request.term.trim() === "") {
                return this.close();
            }
            // Prevent selection of item at current mouse position
            this.menu.previousFilter = "_";
            this.menu.filterTimer = this.menu._delay(function() {
                delete this.previousFilter;
            }, 1000);
            return doSearch(request, response);
        },
        response: function(event, ui) {
            if (!ui.content.length) {
                ui.content.push({ l: messages.noResult });
            }
        },
        autoFocus: true,
        focus: function(event, ui) {
            return false;
        },
        position: {
            collision: "flip"
        },
        select: function(event, ui) {
            if (ui.item.indexItem) {
                var url = getURL(ui.item.indexItem, ui.item.category);
                window.location.href = pathtoroot + url;
                search.blur();
            }
        }
    });
    search.val('');
    search.on("input", () => reset.css("visibility", search.val() ? "visible" : "hidden"))
    search.prop("disabled", false);
    search.attr("autocapitalize", "off");
    reset.prop("disabled", false);
    reset.click(function() {
        search.val('').focus();
    });
});
