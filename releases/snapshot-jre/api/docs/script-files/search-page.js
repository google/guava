/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

"use strict";
$(function() {
    var copy = $("#page-search-copy");
    var expand = $("#page-search-expand");
    var searchLink = $("span#page-search-link");
    var redirect = $("input#search-redirect");
    function setSearchUrlTemplate() {
        var href = document.location.href.split(/[#?]/)[0];
        href += "?q=" + "%s";
        if (redirect.is(":checked")) {
            href += "&r=1";
        }
        searchLink.html(href);
        copy[0].onmouseenter();
    }
    function copyLink(e) {
        copyToClipboard(this.previousSibling.innerText);
        switchCopyLabel(this, this.lastElementChild);
    }
    copy.on("click", copyLink);
    copy[0].onmouseenter = function() {};
    redirect.on("click", setSearchUrlTemplate);
    setSearchUrlTemplate();
    copy.prop("disabled", false);
    redirect.prop("disabled", false);
    expand.on("click", function (e) {
        var searchInfo = $("div.page-search-info");
        if(this.parentElement.hasAttribute("open")) {
            searchInfo.attr("style", " display:none;");
        } else {
            searchInfo.attr("style", "display:block;");
        }
    });
});
$(window).on("load", function() {
    var input = $("#page-search-input");
    var reset = $("#page-search-reset");
    var modules = $("#search-modules");
    var notify = $("#page-search-notify");
    var resultSection = $("div#result-section");
    var resultContainer = $("div#result-container");
    var selectedLink;
    var searchTerm = "";
    var activeTab = "";
    var fixedTab = false;
    var visibleTabs = [];
    var feelingLucky = false;
    const MIN_TABBED_RESULTS = 10;
    function renderResults(result) {
        if (!result.length) {
            notify.html(messages.noResult);
        } else if (result.length === 1) {
            notify.html(messages.oneResult);
        } else {
            notify.html(messages.manyResults.replace("{0}", result.length));
        }
        resultContainer.empty();
        var r = {
            "types": [],
            "members": [],
            "packages": [],
            "modules": [],
            "searchTags": []
        };
        for (var i in result) {
            var item = result[i];
            var arr = r[item.category];
            arr.push(item);
        }
        if (!activeTab || r[activeTab].length === 0) {
            activeTab = Object.keys(r).find(category => r[category].length > 0);
        }
        if (feelingLucky && activeTab) {
            notify.html(messages.redirecting)
            var firstItem = r[activeTab][0];
            window.location = getURL(firstItem.indexItem, firstItem.category);
            return;
        }
        if (searchTerm.endsWith(".") && result.length > MIN_TABBED_RESULTS) {
            if (activeTab === "types" && r["members"].length > r["types"].length) {
                activeTab = "members";
            } else if (activeTab === "packages" && r["types"].length > r["packages"].length) {
                activeTab = "types";
            }
        }
        var categoryCount = Object.keys(r).reduce(function(prev, curr) {
            return prev + (r[curr].length > 0 ? 1 : 0);
        }, 0);
        visibleTabs = [];
        var tabContainer = $("<div class='table-tabs'></div>").appendTo(resultContainer);
        for (var key in r) {
            var id = "#result-tab-" + key.replace("searchTags", "search_tags");
            if (r[key].length) {
                var count = r[key].length >= 1000 ? "999+" : r[key].length;
                if (result.length > MIN_TABBED_RESULTS && categoryCount > 1) {
                    let button = $("<button/>")
                        .attr("id", "result-tab-" + key)
                        .attr("tabIndex", "-1")
                        .addClass("page-search-header")
                        .append($("<span/>")
                            .html(categories[key])
                            .append($("<span/>")
                                .attr("style", "font-weight:normal;")
                                .html(" (" + count + ")")))
                        .on("click", null, key, function(e) {
                            fixedTab = true;
                            renderResult(e.data, $(this));
                        }).appendTo(tabContainer);
                    visibleTabs.push(key);
                } else {
                    $("<span class='page-search-header'>" + categories[key]
                        + "<span style='font-weight: normal'> (" + count + ")</span></span>").appendTo(tabContainer);
                    renderTable(key, r[key]).appendTo(resultContainer);
                    tabContainer = $("<div class='table-tabs'></div>").appendTo(resultContainer);
                }
            }
        }
        if (activeTab && result.length > MIN_TABBED_RESULTS && categoryCount > 1) {
            $("button#result-tab-" + activeTab).addClass("active-table-tab").attr("tabIndex", "0");
            renderTable(activeTab, r[activeTab]).appendTo(resultContainer);
        }
        resultSection.show();
        function renderResult(category, button) {
            activeTab = category;
            setSearchUrl();
            resultContainer.find("div.result-table").remove();
            renderTable(activeTab, r[activeTab]).appendTo(resultContainer);
            button.siblings().removeClass("active-table-tab").attr("tabIndex", "-1");
            button.addClass("active-table-tab").attr("tabIndex", "0");
        }
    }
    function selectTab(category) {
        $("button#result-tab-" + category).focus().trigger("click");
    }
    function renderTable(category, items) {
        var table = $("<div class='result-table'>");
        var col1, col2;
        if (category === "modules") {
            col1 = mdlDesc;
        } else if (category === "packages") {
            col1 = pkgDesc;
        } else if (category === "types") {
            col1 = clsDesc;
        } else if (category === "members") {
            col1 = mbrDesc;
        } else if (category === "searchTags") {
            col1 = tagDesc;
        }
        col2 = descDesc;
        $("<div class='table-header'/>")
            .append($("<span class='table-header'/>").html(col1))
            .append($("<span class='table-header'/>").html(col2))
            .appendTo(table);
        $.each(items, function(index, item) {
            renderItem(item, table);
        });
        return table;
    }
    function select() {
        if (!this.classList.contains("selected")) {
            setSelected(this);
        }
    }
    function unselect() {
        if (this.classList.contains("selected")) {
            setSelected(null);
        }
    }
    function renderItem(item, table) {
        var label = getResultLabel(item);
        var desc = getResultDescription(item);
        var link = $("<a/>")
            .attr("href",  getURL(item.indexItem, item.category))
            .attr("tabindex", "0")
            .addClass("search-result-link");
        link.on("mousemove", select.bind(link[0]))
            .on("focus", select.bind(link[0]))
            .on("mouseleave", unselect.bind(link[0]))
            .on("blur", unselect.bind(link[0]))
            .append($("<span/>").addClass("search-result-label").html(label))
            .append($("<span/>").addClass("search-result-desc").html(desc))
            .appendTo(table);
    }
    var timeout;
    function schedulePageSearch() {
        if (timeout) {
            clearTimeout(timeout);
        }
        timeout = setTimeout(function () {
            doPageSearch()
        }, 100);
    }
    function doPageSearch() {
        setSearchUrl();
        var term = searchTerm = input.val().trim();
        if (term === "") {
            notify.html(messages.enterTerm);
            activeTab = "";
            fixedTab = false;
            resultContainer.empty();
            resultSection.hide();
        } else {
            notify.html(messages.searching);
            var module = modules.val();
            doSearch({ term: term, maxResults: 1200, module: module}, renderResults);
        }
    }
    function setSearchUrl() {
        var query = input.val().trim();
        var url = document.location.pathname;
        if (query) {
            url += "?q=" + encodeURI(query);
            if (activeTab && fixedTab) {
                url += "&c=" + activeTab;
            }
            if (modules.val()) {
                url += "&m=" + modules.val();
            }
        }
        history.replaceState({query: query}, "", url);
    }
    input.on("input", function(e) {
        feelingLucky = false;
        reset.css("visibility", input.val() ? "visible" : "hidden");
        schedulePageSearch();
    });
    function setSelected(link) {
        if (selectedLink) {
            selectedLink.classList.remove("selected");
            selectedLink.blur();
        }
        if (link) {
            link.classList.add("selected");
            link.focus({focusVisible: true});
        }
        selectedLink = link;
    }
    document.addEventListener("keydown", e => {
        if (e.ctrlKey || e.altKey || e.metaKey) {
            return;
        }
        if (e.key === "Escape" && input.val()) {
            input.val("").focus();
            doPageSearch();
            e.preventDefault();
        }
        if (e.target === modules[0]) {
            return;
        }
        if (e.key === "ArrowLeft" || e.key === "ArrowRight") {
            if (activeTab && visibleTabs.length > 1 && e.target !== input[0]) {
                var tab = visibleTabs.indexOf(activeTab);
                var newTab = e.key === "ArrowLeft"
                    ? Math.max(0, tab - 1)
                    : Math.min(visibleTabs.length - 1, tab + 1);
                if (newTab !== tab) {
                    selectTab(visibleTabs[newTab]);
                }
                e.preventDefault();
            }
        } else if (e.key === "ArrowUp" || e.key === "ArrowDown") {
            let links = Array.from(
                document.querySelectorAll("div.result-table > a.search-result-link"));
            let current = links.indexOf(selectedLink);
            let activeButton = document.querySelector("button.active-table-tab");
            if (e.key === "ArrowUp" || (e.key === "Tab" && e.shiftKey)) {
                if (current > 0) {
                    setSelected(links[current - 1]);
                } else {
                    setSelected(null);
                    if (activeButton && current === 0) {
                        activeButton.focus();
                    } else {
                        input.focus();
                    }
                }
            } else if (e.key === "ArrowDown") {
                if (document.activeElement === input[0] && activeButton) {
                    activeButton.focus();
                } else if (current < links.length - 1) {
                    setSelected(links[current + 1]);
                }
            }
            e.preventDefault();
        } else if (e.key.length === 1 || e.key === "Backspace") {
            setSelected(null);
            input.focus();
        }
    });
    reset.on("click", function() {
        notify.html(messages.enterTerm);
        resultSection.hide();
        activeTab = "";
        fixedTab = false;
        resultContainer.empty();
        input.val('').focus();
        setSearchUrl();
    });
    modules.on("change", function() {
        if (input.val()) {
            doPageSearch();
        }
        input.focus();
        try {
            localStorage.setItem("search-modules", modules.val());
        } catch (unsupported) {}
    });

    input.prop("disabled", false);
    input.attr("autocapitalize", "off");
    reset.prop("disabled", false);

    var urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has("m")) {
        modules.val(urlParams.get("m"));
    } else {
        try {
            var searchModules = localStorage.getItem("search-modules");
            if (searchModules) {
                modules.val(searchModules);
            }
        } catch (unsupported) {}
    }
    if (urlParams.has("q")) {
        input.val(urlParams.get("q"));
        reset.css("visibility", input.val() ? "visible" : "hidden");
    }
    if (urlParams.has("c")) {
        activeTab = urlParams.get("c");
        fixedTab = true;
    }
    if (urlParams.get("r")) {
        feelingLucky = true;
    }
    if (input.val()) {
        doPageSearch();
    } else {
        notify.html(messages.enterTerm);
    }
    input.select().focus();
});
