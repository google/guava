/*
 * Copyright (c) 2015, 2026, Oracle and/or its affiliates. All rights reserved.
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
const linkLabel = "Search page";
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
            for (var i = 0; i < packageSearchIndex.length; i++) {
                const it = packageSearchIndex[i];
                if (it.m && item.p === it.l) {
                    urlPrefix = it.m + slash;
                    item.m = it.m;
                    break;
                }
            }
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
        indexArray.forEach(function (item) {
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
        for (let i = 0; i < typeSearchIndex.length; i++){
            const it = typeSearchIndex[i];
            if (it.l === item.c && it.p === item.p && it.m === item.m) {
                item.typeDesc = itemDesc[it.k || 12][1];
                break;
            }
        }
    }
    return item.typeDesc || "";
}

window.addEventListener("load", () => {
    var copy = document.querySelector("#page-search-copy");
    if (!copy) {
        return;
    }
    var expand = document.querySelector("#page-search-expand");
    var searchLink = document.querySelector("span#page-search-link");
    var redirect = document.querySelector("input#search-redirect");
    function setSearchUrlTemplate() {
        var href = document.location.href.split(/[#?]/)[0];
        href += "?q=" + "%s";
        if (redirect.checked) {
            href += "&r=1";
        }
        searchLink.innerHTML = href;
        copy.onmouseenter();
    }
    function copyLink(e) {
        copyToClipboard(this.previousSibling.innerText);
        switchCopyLabel(this, this.lastElementChild);
    }
    copy.addEventListener("click", copyLink.bind(copy));
    copy.onmouseenter = function() {};
    redirect.addEventListener("click", setSearchUrlTemplate);
    setSearchUrlTemplate();
    copy.disabled = false;
    redirect.disabled = false;
    expand.addEventListener("click", function (e) {
        var searchInfo = document.querySelector("div.page-search-info");
        if(this.parentElement.hasAttribute("open")) {
            searchInfo.setAttribute("style", " display:none;");
        } else {
            searchInfo.setAttribute("style", "display:block;");
        }
    });
});
window.addEventListener("load", () => {
    const empty = (el) => { if (el) el.replaceChildren(); };
    const setContent = (el, text) => { if (el) el.textContent = text; };

    const input = document.querySelector("#search-input, #page-search-input");
    const reset = document.querySelector("#reset-search, #page-search-reset");
    const modules = document.querySelector("#search-modules");
    const notify = document.querySelector("#page-search-notify");
    const resultSection = document.querySelector("#search-result-section");
    const resultContainer = document.querySelector("#search-result-container");

    let overlay = null;
    let selectedLink = null;

    let searchTerm = "";
    let activeTab = "";
    let fixedTab = false;
    let visibleTabs = [];
    let redirect = false;

    const MIN_TABBED_RESULTS = 10;

    function renderResults(result) {
        empty(resultContainer);

        if (!result.length) {
            empty(notify);
            const p = document.createElement("p");
            p.textContent = messages.noResult;
            resultContainer.appendChild(p);
            return;
        } else if (result.length === 1) {
            setContent(notify, messages.oneResult);
        } else {
            setContent(notify, messages.manyResults.replace("{0}", String(result.length)));
        }

        const r = { types: [], members: [], packages: [], modules: [], searchTags: [] };
        for (const item of result) (r[item.category] ??= []).push(item);

        if (!activeTab || r[activeTab].length === 0) {
            activeTab = Object.keys(r).find(category => r[category].length > 0) || "";
        }

        if (redirect && activeTab) {
            setContent(notify, messages.redirecting);
            const firstItem = r[activeTab][0];
            window.location = pathtoroot + getURL(firstItem.indexItem, firstItem.category);
            input.value = "";
            return;
        }

        if (searchTerm.endsWith(".") && result.length > MIN_TABBED_RESULTS) {
            if (activeTab === "types" && r.members.length > r.types.length) {
                activeTab = "members";
            } else if (activeTab === "packages" && r.types.length > r.packages.length) {
                activeTab = "types";
            }
        }

        const categoryCount = Object.keys(r).reduce(
            (prev, curr) => prev + (r[curr].length > 0 ? 1 : 0),
            0
        );

        visibleTabs = [];

        let tableTabs = document.createElement("div");
        tableTabs.className = "table-tabs";

        const resultTable = document.createElement("div");
        resultTable.className = "result-table";

        for (const key in r) {
            if (!r[key].length) continue;

            const count = r[key].length >= 1000 ? "1000+" : String(r[key].length);

            if (result.length > MIN_TABBED_RESULTS && categoryCount > 1) {
                const btn = document.createElement("button");
                btn.id = `result-tab-${key}`;
                btn.tabIndex = -1;
                btn.classList.add("page-search-header");

                const labelSpan = document.createElement("span");
                setContent(labelSpan, categories[key]);

                const countSpan = document.createElement("span");
                countSpan.style.fontWeight = "normal";
                countSpan.textContent = ` (${count})`;

                labelSpan.appendChild(countSpan);
                btn.appendChild(labelSpan);

                btn.addEventListener("click", () => {
                    fixedTab = true;
                    renderResult(key, btn);
                });

                tableTabs.appendChild(btn);
                visibleTabs.push(key);
            } else {
                const header = document.createElement("span");
                header.className = "page-search-header";
                header.appendChild(document.createTextNode(categories[key]));

                const countSpan = document.createElement("span");
                countSpan.style.fontWeight = "normal";
                countSpan.textContent = ` (${count})`;
                header.appendChild(countSpan);

                tableTabs.appendChild(header);

                (categoryCount > 1 ? resultTable : resultContainer).appendChild(tableTabs);

                renderItems(r[key], resultTable);

                tableTabs = document.createElement("div");
                tableTabs.className = "table-tabs";
            }
        }

        if (activeTab && result.length > MIN_TABBED_RESULTS && categoryCount > 1) {
            resultContainer.appendChild(tableTabs);

            const activeBtn = document.querySelector(`button#result-tab-${activeTab}`);
            if (activeBtn) {
                activeBtn.classList.add("active-table-tab");
                activeBtn.tabIndex = 0;
            }

            renderItems(r[activeTab], resultTable);
        }

        resultContainer.appendChild(resultTable);

        function renderResult(category, button) {
            activeTab = category;

            Array.from(resultContainer.querySelectorAll("div.result-table")).forEach(el => el.remove());

            const newTable = renderItems(r[activeTab]);
            resultContainer.appendChild(newTable);

            const siblings = Array.from(button.parentElement?.children ?? []).filter(n => n !== button);
            siblings.forEach(sib => {
                sib.classList?.remove("active-table-tab");
                if (sib instanceof HTMLElement) sib.tabIndex = -1;
            });

            button.classList.add("active-table-tab");
            button.tabIndex = 0;
            setSearchUrl();
        }
    }

    function selectTab(category) {
        const btn = document.querySelector(`button#result-tab-${category}`);
        if (!btn) return;
        btn.focus();
        btn.dispatchEvent(new MouseEvent("click"));
    }

    function select() {
        if (!this.classList.contains("selected")) setSelected(this);
    }

    function unselect() {
        if (this.classList.contains("selected")) setSelected(null);
    }

    function renderItems(items, table) {
        if (!table) {
            table = document.createElement("div");
            table.className = "result-table";
        }
        items.forEach(item => renderItem(item, table));
        return table;
    }

    function renderItem(item, table) {
        const label = getResultLabel(item);
        const desc = getResultDescription(item);

        const link = document.createElement("a");
        link.href = pathtoroot + getURL(item.indexItem, item.category);
        link.tabIndex = 0;
        link.className = "search-result-link";

        link.addEventListener("focus", select.bind(link));
        link.addEventListener("blur", unselect.bind(link));
        link.addEventListener("click", closeSearch);

        const labelSpan = document.createElement("span");
        labelSpan.className = "search-result-label";
        labelSpan.innerHTML = label;

        const descSpan = document.createElement("span");
        descSpan.className = "search-result-desc";
        descSpan.innerHTML = desc;

        link.appendChild(labelSpan);
        link.appendChild(descSpan);
        table.appendChild(link);
    }

    let timeout;
    function scheduleSearch() {
        if (timeout) clearTimeout(timeout);
        timeout = setTimeout(search, 200);
    }

    function search() {
        setSearchUrl();
        const term = (searchTerm = input.value.trim());
        if (term === "") {
            clearResult();
        } else {
            setContent(notify, messages.searching);
            const module = modules ? modules.value : "";
            doSearch({ term, maxResults: 1200, module }, renderResults);
        }
    }

    function setSearchUrl() {
        var query = input.value.trim();
        if (input.id === "search-input") {
            var link = document.getElementById("search-page-link");
            var href = pathtoroot + "search.html?q=" + encodeURI(query);
            if (activeTab && fixedTab) {
                href += "&c=" + activeTab;
            }
            if (modules && modules.value) {
                href += "&m=" + modules.value;
            }
            link.href = href;
        } else {
            var url = document.location.pathname;
            if (query) {
                url += "?q=" + encodeURI(query);
                if (activeTab && fixedTab) {
                    url += "&c=" + activeTab;
                }
                if (modules && modules.value) {
                    url += "&m=" + modules.value;
                }
            }
            history.replaceState({ query: query }, "", url);
        }
    }

    input.addEventListener("input", () => {
        redirect = false;
        reset.style.visibility = input.value ? "visible" : "hidden";
        scheduleSearch();
    });

    input.addEventListener("focus", openSearch);

    input.addEventListener("mouseup", function(e) {
        e.preventDefault();
    });

    function setSelected(link) {
        if (selectedLink) {
            selectedLink.classList.remove("selected");
            selectedLink.blur();
        }
        if (link) {
            link.classList.add("selected");
            link.focus({ focusVisible: true });
            link.scrollIntoView({ block: "nearest" });
        }
        selectedLink = link;
    }

    function clearResult() {
        setContent(notify, messages.enterTerm);
        activeTab = "";
        fixedTab = false;
        empty(resultContainer);
        reset.style.visibility = input.value ? "visible" : "hidden";
        setSearchUrl();
    }

    function openSearch() {
        resultSection.style.display = "block";

        if (input.id !== "search-input" || overlay) return;

        overlay = document.createElement("div");
        overlay.className = "overlay";
        document.querySelector("header")?.appendChild(overlay);
        overlay.addEventListener("click", closeSearch);
        overlay.style.display = "block";

        document.body.style.setProperty("overflow-y", "hidden");

        var inputDiv = document.getElementById("search-input-container");
        inputDiv.appendChild(input);
        inputDiv.appendChild(reset);
        input.focus();

        if (input.value) {
            input.select();
            search();
        }
    }

    function closeSearch() {
        clearResult();
        resultSection.style.display = "none";

        if (overlay) {
            var inputDiv = document.querySelector("div.sub-nav div.nav-list-search");
            inputDiv.appendChild(input);
            inputDiv.appendChild(reset);

            overlay.remove();
            overlay = null;
            document.body.style.removeProperty("overflow-y");
        }
    }

    window.addEventListener("hashchange", closeSearch);

    document.addEventListener("keydown", (e) => {
        if (e.ctrlKey || e.altKey || e.metaKey || resultSection.style.display !== "block") return;

        if (e.key === "Escape") {
            if (input.value) {
                input.value = "";
                input.focus();
                clearResult();
            } else {
                closeSearch();
                input.blur();
            }
            e.preventDefault();
            return;
        }

        if (e.target === modules) return;

        if (e.key === "ArrowLeft" || e.key === "ArrowRight") {
            if (activeTab && visibleTabs.length > 1 && e.target !== input) {
                const tab = visibleTabs.indexOf(activeTab);
                const newTab = e.key === "ArrowLeft"
                    ? Math.max(0, tab - 1)
                    : Math.min(visibleTabs.length - 1, tab + 1);

                if (newTab !== tab) selectTab(visibleTabs[newTab]);
                e.preventDefault();
            }
        } else if (e.key === "ArrowUp" || e.key === "ArrowDown") {
            const links = Array.from(document.querySelectorAll("div.result-table > a.search-result-link"));
            const current = links.indexOf(selectedLink);

            if (e.key === "ArrowUp" || (e.key === "Tab" && e.shiftKey)) {
                if (current > 0) {
                    setSelected(links[current - 1]);
                } else {
                    setSelected(null);
                    input.focus();
                }
            } else if (e.key === "ArrowDown") {
                if (current < links.length - 1) setSelected(links[current + 1]);
            }
            e.preventDefault();
        } else if (e.target !== input && (e.key.length === 1 || e.key === "Backspace")) {
            setSelected(null);
            input.focus();
            e.preventDefault();
        }
    });

    reset.addEventListener("click", () => {
        input.value = "";
        input.focus();
        clearResult();
    });

    if (modules) {
        modules.addEventListener("change", () => {
            if (input.value) search();
            input.focus();
            try {
                localStorage.setItem("search-modules", modules.value);
            } catch (unsupported) {
                console.log("Error setting module: " + unsupported);
            }
        });
    }

    // Enable/initialize
    input.disabled = false;
    input.setAttribute("autocapitalize", "off");
    reset.disabled = false;

    var urlParams = new URLSearchParams(window.location.search);
    if (modules) {
        if (urlParams.has("m")) {
            modules.value = urlParams.get("m");
        } else {
            try {
                const searchModules = localStorage.getItem("search-modules");
                if (searchModules) modules.value = searchModules;
            } catch (unsupported) {
                console.log("Error getting module: " + unsupported);
            }
        }
    }
    if (urlParams.has("q")) {
        input.value = urlParams.get("q");
        reset.style.visibility = input.value ? "visible" : "hidden";
    }
    if (urlParams.has("c")) {
        activeTab = urlParams.get("c");
        fixedTab = true;
    }
    if (urlParams.get("r")) {
        redirect = true;
    }
    if (input.value) {
        search();
    } else {
        setContent(notify, messages.enterTerm);
    }
    if (input.id === "page-search-input") {
        input.select();
        input.focus();
    }
});
