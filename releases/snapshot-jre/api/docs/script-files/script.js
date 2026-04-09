/*
 * Copyright (c) 2013, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

var moduleSearchIndex;
var packageSearchIndex;
var typeSearchIndex;
var memberSearchIndex;
var tagSearchIndex;

const oddRowColor = "odd-row-color";
const evenRowColor = "even-row-color";
const sortAsc = "sort-asc";
const sortDesc = "sort-desc";
const tableTab = "table-tab";
const activeTableTab = "active-table-tab";
const THEME_LIGHT = "theme-light";
const THEME_DARK = "theme-dark";
const THEME_OS = "theme-os";

const linkIcon = "Link icon";
const linkToSection = "Link to this section";
const toggleMemberListing = "Toggle between short and detailed list view";

const sortLexicalLabel = "Sort member details lexicographically";
const sortSourceLabel  = "Sort member details by source order";
const TOC_ALPHA  = "alpha";
const TOC_SOURCE = "source";
var origOlOrder = new Map();
var origContainerOrder = new Map();
var snapshotted = false;

if (typeof hljs !== "undefined") {
    try {
        hljs.highlightAll();
    } catch (err) {
        console.error(err)
    }
}

function loadScripts() {
    createScript('script-files/search.js');
    createScript('module-search-index.js');
    createScript('package-search-index.js');
    createScript('type-search-index.js');
    createScript('member-search-index.js');
    createScript('tag-search-index.js');
}

function createScript(path) {
    var script = document.createElement("script");
    script.src = pathtoroot + path;
    var firstScript = document.getElementsByTagName("script")[0];
    firstScript.parentNode.insertBefore(script, firstScript);
}

// Helper for  making content containing release names comparable lexicographically
function makeComparable(s) {
    return s.toLowerCase().replace(/(\d+)/g,
        function(n, m) {
            return ("000" + m).slice(-4);
        });
}

// Switches between two styles depending on a condition
function toggleStyle(classList, condition, trueStyle, falseStyle) {
    if (condition) {
        classList.remove(falseStyle);
        classList.add(trueStyle);
    } else {
        classList.remove(trueStyle);
        classList.add(falseStyle);
    }
}

// Sorts the rows in a table lexicographically by the content of a specific column
function sortTable(header, columnIndex, columns) {
    var container = header.parentElement;
    var descending = header.classList.contains(sortAsc);
    container.querySelectorAll("div.table-header").forEach(
        function(header) {
            header.classList.remove(sortAsc);
            header.classList.remove(sortDesc);
        }
    )
    var cells = container.children;
    var rows = [];
    for (var i = columns; i < cells.length; i += columns) {
        rows.push(Array.prototype.slice.call(cells, i, i + columns));
    }
    var comparator = function(a, b) {
        var ka = makeComparable(a[columnIndex].textContent);
        var kb = makeComparable(b[columnIndex].textContent);
        if (ka < kb)
            return descending ? 1 : -1;
        if (ka > kb)
            return descending ? -1 : 1;
        return 0;
    };
    var sorted = rows.sort(comparator);
    var visible = 0;
    sorted.forEach(function(row) {
        if (row[0].style.display !== 'none') {
            var isEvenRow = visible++ % 2 === 0;
        }
        row.forEach(function(cell) {
            toggleStyle(cell.classList, isEvenRow, evenRowColor, oddRowColor);
            container.appendChild(cell);
        })
    });
    toggleStyle(header.classList, descending, sortDesc, sortAsc);
}

// Toggles the visibility of a table category in all tables in a page
function toggleGlobal(checkbox, selected, columns) {
    const display = checkbox.checked ? '' : 'none';
    const selectOther = selected === "other";
    const selectAll = selected === "all";
    if (selectAll) {
        document.querySelectorAll('.checkboxes input[type="checkbox"]').forEach(c => {
            c.checked = checkbox.checked;
        });
    }
    document.querySelectorAll("div.table-tabs").forEach(t => {
        const id = t.parentElement.getAttribute("id");
        const selectedClass = id + "-tab" + (selectOther ? "" : selected);
        var visible = 0;
        t.parentElement.querySelectorAll('div.' + id)
            .forEach(function(elem) {
                if (selectAll
                    || (!selectOther && elem.classList.contains(selectedClass))
                    || (selectOther && elem.className.indexOf(selectedClass) < 0)) {
                    elem.style.display = display;
                }
                if (elem.style.display === '') {
                    var isEvenRow = visible++ % (columns * 2) < columns;
                    toggleStyle(elem.classList, isEvenRow, evenRowColor, oddRowColor);
                }
            });
        var displaySection = visible === 0 ? 'none' : '';
        t.parentElement.style.display = displaySection;
        document.querySelector("li#contents-" + id).style.display = displaySection;
    })
}

// Shows the elements of a table belonging to a specific category
function show(tableId, selected, columns) {
    if (tableId !== selected) {
        document.querySelectorAll('div.' + tableId + ':not(.' + selected + ')')
            .forEach(function(elem) {
                elem.style.display = 'none';
            });
    }
    document.querySelectorAll('div.' + selected)
        .forEach(function(elem, index) {
            elem.style.display = '';
            var isEvenRow = index % (columns * 2) < columns;
            toggleStyle(elem.classList, isEvenRow, evenRowColor, oddRowColor);
        });
    updateTabs(tableId, selected);
}

function updateTabs(tableId, selected) {
    document.getElementById(tableId + '.tabpanel')
        .setAttribute('aria-labelledby', selected);
    document.querySelectorAll('button[id^="' + tableId + '"]')
        .forEach(function(tab, index) {
            if (selected === tab.id || (tableId === selected && index === 0)) {
                tab.className = activeTableTab;
                tab.setAttribute('aria-selected', true);
                tab.setAttribute('tabindex',0);
            } else {
                tab.className = tableTab;
                tab.setAttribute('aria-selected', false);
                tab.setAttribute('tabindex',-1);
            }
        });
}

function switchTab(e) {
    var selected = document.querySelector('[aria-selected=true]');
    if (selected) {
        if ((e.keyCode === 37 || e.keyCode === 38) && selected.previousSibling) {
            // left or up arrow key pressed: move focus to previous tab
            selected.previousSibling.click();
            selected.previousSibling.focus();
            e.preventDefault();
        } else if ((e.keyCode === 39 || e.keyCode === 40) && selected.nextSibling) {
            // right or down arrow key pressed: move focus to next tab
            selected.nextSibling.click();
            selected.nextSibling.focus();
            e.preventDefault();
        }
    }
}

var updateSearchResults = function() {};

function indexFilesLoaded() {
    return moduleSearchIndex
        && packageSearchIndex
        && typeSearchIndex
        && memberSearchIndex
        && tagSearchIndex;
}
// Copy the contents of the local snippet to the clipboard
function copySnippet(button) {
    copyToClipboard(button.nextElementSibling.innerText);
    switchCopyLabel(button, button.firstElementChild);
}
function copyToClipboard(content) {
    var textarea = document.createElement("textarea");
    textarea.style.height = 0;
    document.body.appendChild(textarea);
    textarea.value = content;
    textarea.select();
    document.execCommand("copy");
    document.body.removeChild(textarea);
}
function resetInput(input, event, blur) {
    if (input.value) {
        input.value = "";
        input.dispatchEvent(new InputEvent("input"));
    } else if (blur) {
        input.blur();
    }
    event.preventDefault();
}
function isInput(elem) {
    return elem instanceof HTMLInputElement && elem.type === "text";
}
function switchCopyLabel(button, span) {
    var copied = span.getAttribute("data-copied");
    button.classList.add("visible");
    var initialLabel = span.innerHTML;
    span.innerHTML = copied;
    setTimeout(function() {
        button.classList.remove("visible");
        setTimeout(function() {
            if (initialLabel !== copied) {
                span.innerHTML = initialLabel;
            }
        }, 100);
    }, 1900);
}
function makeFilterWidget(sidebar, updateToc) {
    if (!sidebar) {
        return null;
    }
    const filterInput = sidebar.querySelector("input.filter-input");
    const resetInput = sidebar.querySelector("input.reset-filter");
    sidebar.addEventListener("keydown", e => {
        if (e.ctrlKey || e.altKey || e.metaKey) {
            return;
        }
        if (e.key === "ArrowUp" || e.key === "ArrowDown") {
            handleTocFocus(e);
        } else if (filterInput && e.target !== filterInput) {
            if (e.key === "Enter" && isTocLink(sidebar, e.target)) {
                filterInput.value = "";
                filterInput.dispatchEvent(new InputEvent("input"));
            } else if (e.key.length === 1 || e.key === "Backspace") {
                filterInput.focus();
            }
        }
    });
    if (filterInput) {
        filterInput.removeAttribute("disabled");
        filterInput.setAttribute("autocapitalize", "off");
        filterInput.value = "";
        filterInput.addEventListener("input", function(e) {
            resetInput.style.visibility = filterInput.value ? "visible" : "hidden";
            const pattern = filterInput.value ? filterInput.value.trim()
                .replace(/[\[\]{}()*+?.\\^$|]/g, '\\$&')
                .replace(/\s+/g, ".*") : "";
            const filter = new RegExp(pattern, "i");
            sidebar.querySelectorAll("ol.toc-list li").forEach((li) => {
                if (filter.test(li.innerText)) {
                    // li.removeAttribute("style");
                    const selfMatch = filter.test(li.firstElementChild.innerText);
                    li.style.display = "block";
                    li.firstElementChild.style.opacity =  selfMatch ? "100%" : "70%";
                    li.firstElementChild.tabIndex = selfMatch ? 0 : -1;
                } else {
                    li.style.display = "none";
                }
            });
            updateToc();
        });
    }
    if (resetInput) {
        resetInput.removeAttribute("disabled");
        resetInput.addEventListener("click", (e) => {
            filterInput.value = "";
            filterInput.focus();
            filterInput.dispatchEvent(new InputEvent("input"));
        });
    }
    function handleTocFocus(event) {
        let links = Array.from(sidebar.querySelectorAll("ol > li > a"))
            .filter(link => link.offsetParent && link.tabIndex === 0);
        let current = links.indexOf(document.activeElement);
        if (event.key === "ArrowUp") {
            if (current > 0) {
                links[current - 1].focus({focusVisible: true});
            } else if (filterInput) {
                filterInput.focus();
            }
        } else if (event.key === "ArrowDown" && current < links.length - 1) {
            links[current + 1].focus({focusVisible: true});
        }
        event.preventDefault();
    }
    function isTocLink(sidebar, elem) {
        let links = Array.from(sidebar.querySelectorAll("ol > li > a"))
            .filter(link => link.offsetParent && link.tabIndex === 0);
        return links.indexOf(elem) > -1;
    }
    return sidebar;
}

const osDarkTheme = window.matchMedia("(prefers-color-scheme: dark)");
osDarkTheme.addEventListener("change", e => {
    if (getTheme() === THEME_OS) {
        setTheme(THEME_OS);
    }
});
function getTheme() {
    return localStorage.getItem("theme") || THEME_LIGHT;
}
function setTheme(theme) {
    var value = theme !== THEME_OS ? theme : osDarkTheme.matches ? THEME_DARK : THEME_LIGHT;
    document.documentElement.setAttribute("data-theme", value);
}
function initTheme() {
    setTheme(getTheme());
}

function setTopMargin() {
    // Dynamically set scroll margin to accomodate for draft header
    var headerHeight = Math.ceil(document.querySelector("header").offsetHeight);
    document.querySelector(":root")
        .style.setProperty("--nav-height", headerHeight + "px");
}

document.addEventListener("readystatechange", (e) => {
    if (document.readyState === "interactive") {
        setTopMargin();
        if (sessionStorage.getItem("sidebar") === "hidden") {
            const sidebar = document.querySelector(".main-grid nav.toc");
            if (sidebar) sidebar.classList.add("hide-sidebar");
        }
    }
});

document.addEventListener("DOMContentLoaded", function(e) {
    setTopMargin();
    const keymap = new Map();
    const searchInput = document.getElementById("search-input")
                   || document.getElementById("page-search-input");
    if (searchInput) {
        searchInput.addEventListener("focus", collapse);
        keymap.set("/", searchInput);
    }
    const filterInput = document.querySelector("input.filter-input");
    if (filterInput) {
        keymap.set(".", filterInput);
    }
    // Clone TOC sidebar to header for mobile navigation
    const sidebar = document.querySelector(".main-grid nav.toc");
    const main = document.querySelector(".main-grid main");
    const mainnav = document.querySelector("div#navbar-top ul.nav-list");
    const toggleButton = document.querySelector("button#navbar-toggle-button");
    const tocMenu = sidebar ? sidebar.cloneNode(true) : null;
    const themeButton = document.querySelector("button#theme-button");
    const themePanel = document.querySelector("div#theme-panel");
    var themePanelVisible = false;
    themeButton.addEventListener("click", e => {
        if (!themePanelVisible) {
            let {x, y} = themeButton.getBoundingClientRect();
            themePanel.style.display = "block";
            if (window.innerHeight - 85 < y) {
                themePanel.style.top = "";
                themePanel.style.bottom = "4px";
            } else {
                themePanel.style.top = y + (expanded ? 0 : 36) + "px";
                themePanel.style.bottom = "";
            }
            themePanel.style.left = x + (expanded ? 36 : 0) + "px";
            themeButton.setAttribute("aria-expanded", "true");
            themePanelVisible = true;
            e.stopPropagation();
        }
    });
    function closeThemePanel(e) {
        if (themePanelVisible && (!e || !themePanel.contains(e.target))) {
            themePanel.style.removeProperty("display");
            themeButton.setAttribute("aria-expanded", "false");
            themePanelVisible = false;
        }
    }
    var currentTheme = getTheme();
    themePanel.querySelectorAll("input").forEach(input => {
        input.removeAttribute("disabled");
        if (input.id === currentTheme) {
            input.checked = true;
        }
        input.addEventListener("change", e => {
            setTheme(e.target.value);
            localStorage.setItem("theme", e.target.value);
        })
    });
    themePanel.addEventListener("focusout", e => {
        if (e.relatedTarget && !themePanel.contains(e.relatedTarget) && !themeButton.contains(e.relatedTarget)) {
            closeThemePanel();
        }
    });
    themePanel.addEventListener("keydown", e => {
        if (e.key === "Escape" || e.key === "Enter") {
            closeThemePanel();
        }
    });
    makeFilterWidget(sidebar, updateToc);
    if (tocMenu) {
        document.querySelector("div#navbar-top").appendChild(tocMenu);
        makeFilterWidget(tocMenu, updateToc);
        var menuInput = tocMenu.querySelector("input.filter-input");
    }

    snapshotAllOnce();

    document.addEventListener("keydown", (e) => {
        if (e.ctrlKey || e.altKey || e.metaKey) {
            return;
        }
        if (!isInput(e.target) && keymap.has(e.key)) {
            closeThemePanel();
            var elem = keymap.get(e.key);
            if (elem === filterInput && !elem.offsetParent) {
                elem = getVisibleFilterInput(true);
            }
            elem.focus();
            elem.select();
            e.preventDefault();
        } else if (e.key === "Escape") {
            closeThemePanel();
            if (expanded) {
                collapse();
                e.preventDefault();
            } else if (e.target.id === "page-search-input") {
                resetInput(e.target, e, false);
            } else if (isInput(e.target)) {
                resetInput(e.target, e, true);
            } else {
                var filter = getVisibleFilterInput(false);
                if (filter && filter.value) {
                    resetInput(filterInput, e, true);
                }
            }
        }
    });
    var expanded = false;
    var windowWidth;
    var bodyHeight;
    function collapse() {
        closeThemePanel();
        if (expanded) {
            mainnav.removeAttribute("style");
            if (tocMenu) {
                tocMenu.removeAttribute("style");
                if (filterInput) {
                    keymap.set(".", filterInput);
                }
            }
            toggleButton.classList.remove("expanded")
            toggleButton.setAttribute("aria-expanded", "false");
            expanded = false;
        }
    }
    function expand() {
        closeThemePanel();
        expanded = true;
        mainnav.style.display = "block";
        mainnav.style.removeProperty("height");
        var maxHeight = window.innerHeight - document.querySelector("div.sub-nav").offsetTop;
        var expandedHeight = Math.min(maxHeight, mainnav.scrollHeight + 10);
        if (tocMenu) {
            tocMenu.style.display = "flex";
            expandedHeight = Math.min(maxHeight,
                Math.max(expandedHeight, tocMenu.querySelector("div.toc-header").offsetHeight
                                       + tocMenu.querySelector("ol.toc-list").scrollHeight + 10));
            tocMenu.style.height = expandedHeight + "px";
            if (menuInput) {
                keymap.set(".", menuInput);
            }
        }
        mainnav.style.height = expandedHeight + "px";
        toggleButton.classList.add("expanded");
        toggleButton.setAttribute("aria-expanded", "true");
        windowWidth = window.innerWidth;
    }
    function updateToc() {
        if (expanded) {
            expand();
        } else {
            prevHash = null;
            handleScroll();
        }
    }
    function getVisibleFilterInput(show) {
        if (sidebar && sidebar.offsetParent) {
            if (show) {
                showSidebar();
            }
            return filterInput;
        } else {
            if (show) {
                expand();
            }
            return menuInput;
        }
    }
    toggleButton.addEventListener("click", (e) => {
        if (expanded) {
            collapse();
        } else {
            expand();
        }
    });
    if (tocMenu) {
        tocMenu.querySelectorAll("a").forEach((link) => {
            link.addEventListener("click", collapse);
        });
    }
    document.querySelector("main").addEventListener("click", collapse);
    document.querySelector("body").addEventListener("click", closeThemePanel);
    document.querySelectorAll("h1, h2, h3, h4, h5, h6")
        .forEach((hdr, idx) => {
            // Create anchor links for headers with an associated id attribute
            var id = hdr.parentElement.getAttribute("id") || hdr.getAttribute("id")
                || (hdr.querySelector("a") && hdr.querySelector("a").getAttribute("id"));
            if (id) {
                var template = document.createElement('template');
                template.innerHTML =" <a href='#" + encodeURI(id) + "' class='anchor-link' aria-label='"
                    + linkToSection + "'><img src='" + pathtoroot + "resource-files/link.svg' alt='"
                    + linkIcon + "' width='16' height='16'></a>";
                hdr.append(...template.content.childNodes);
            }
        });
    document.querySelectorAll("div.inherited-list:has( > div.summary-table) > h3").forEach(h => {
        const button = document.createElement("button");
        const p = h.parentElement;
        button.addEventListener("click", e => {
            toggleStyle(p.classList, p.classList.contains("expanded"), "collapsed", "expanded");
        });
        button.addEventListener("keydown", e => {
            if (e.key === " " || e.key === "Enter") {
                toggleStyle(p.classList, p.classList.contains("expanded"), "collapsed", "expanded");
                e.preventDefault();
            }
        });
        button.setAttribute("aria-label", toggleMemberListing);
        h.insertAdjacentElement("afterbegin", button);
    });
    var sections;
    var scrollTimeout;
    var prevHash;
    function initSectionData() {
        bodyHeight = document.body.offsetHeight;
        sections = [{ id: "", top: 0 }].concat(
            Array.from(main.querySelectorAll("section[id], h2[id], h2 a[id], h3[id], h3 a[id], div[id]"))
                .filter((e) => {
                    var id = encodeURI(e.getAttribute("id"));
                    return sidebar.querySelector("a[href=\"#" + id + "\"]") !== null
                }).map((e) => {
                    return {
                        id: e.getAttribute("id"),
                       top: e.offsetTop
                    };
                })
        );
    }
    function setScrollTimeout() {
        if (scrollTimeout) {
            clearTimeout(scrollTimeout);
        }
        scrollTimeout = setTimeout(() => {
            scrollTimeout = null;
        }, 100);
    }
    function handleScroll() {
        if (!sidebar || !sidebar.offsetParent || sidebar.classList.contains("hide-sidebar")) {
            return;
        }
        if (scrollTimeout) {
            setScrollTimeout();
            return;
        }
        var scrollTop = document.documentElement.scrollTop;
        var scrollHeight = document.documentElement.scrollHeight;
        var currHash = null;
        for (var i = 0; i < sections.length; i++) {
            var top = sections[i].top;
            var bottom = sections[i + 1] ? sections[i + 1].top : scrollHeight;
            if (top + ((bottom - top) / 2) > scrollTop || bottom > scrollTop + (window.innerHeight / 3)) {
                currHash = "#" + encodeURI(sections[i].id);
                break;
            }
        }
        if (currHash !== prevHash) {
            setSelected(currHash);
        }
    }
    function setSelected(hash) {
        var prev = sidebar.querySelector("a.current-selection");
        if (prev)
            prev.classList.remove("current-selection");
        prevHash = hash;
        if (hash) {
            var curr = sidebar.querySelector("ol.toc-list a[href=\"" + hash + "\"]");
            if (curr) {
                curr.classList.add("current-selection");
                curr.scrollIntoView({ behavior: "instant", block: "nearest" });
            }
        }
    }
    function hideSidebar() {
        sidebar.classList.add("hide-sidebar");
        sessionStorage.setItem("sidebar", "hidden");
    }
    function showSidebar() {
        sidebar.classList.remove("hide-sidebar");
        sessionStorage.removeItem("sidebar");
        initSectionData();
        handleScroll();
    }
    if (sidebar) {
        initSectionData();
        document.querySelectorAll("a[href^='#']").forEach((link) => {
            link.addEventListener("click", (e) => {
                link.blur();
                setScrollTimeout();
                setSelected(link.getAttribute("href"));
            })
        });
        sidebar.querySelector("button.hide-sidebar").addEventListener("click", hideSidebar);
        sidebar.querySelector("button.show-sidebar").addEventListener("click", showSidebar);
        window.addEventListener("hashchange", (e) => {
            setScrollTimeout();
            const hash = e.newURL.indexOf("#");
            if (hash > -1) {
                setSelected(e.newURL.substring(hash));
            }
        });
        if (document.location.hash) {
            setScrollTimeout();
            setSelected(document.location.hash);
        } else {
            handleScroll();
        }
        window.addEventListener("scroll", handleScroll);
    }

    function allTocNavs() {
        return Array.from(document.querySelectorAll("nav.toc"));
    }

    function nestedTocLists(scope) {
        const listsToSort = [];
        const sectionLinks = scope.querySelectorAll("ol.toc-list > li > a");

        sectionLinks.forEach(function(link) {
            const href = link.getAttribute("href");
            if (href === "#constructor-detail" || href === "#method-detail"
                || href === "#field-detail" || href === "#annotation-interface-element-detail"
                || href === "#enum-constant-detail" || href === "#property-detail") {
                const memberList = link.nextElementSibling;
                if (memberList && memberList.tagName === 'OL') {
                    listsToSort.push(memberList);
                }
            }
        });

        return listsToSort;
    }

    function textForLi(li) {
        return li.querySelector(":scope > a").textContent.trim();
    }

    function alphaCompare(a, b) {
        return textForLi(a).localeCompare(textForLi(b), undefined, {
            numeric: true,
            sensitivity: "base"
        });
    }

    function snapshotTocOnce(nav){
        nestedTocLists(nav).forEach(function(ol){
            if (!origOlOrder.has(ol)) origOlOrder.set(ol, Array.from(ol.children));
        });
    }
    function restoreToc(nav){
        nestedTocLists(nav).forEach(function(ol){
            var orig = origOlOrder.get(ol);
            if (orig) orig.forEach(function(li){ ol.appendChild(li); });
        });
    }
    function sortTocAlpha(nav){
        nestedTocLists(nav).forEach(function(ol){
            var lis = Array.from(ol.children);
            if (lis.length < 2) return;
            lis.slice().sort(alphaCompare).forEach(function(li){ ol.appendChild(li); });
        });
    }

    function snapshotAllOnce() {
        if (snapshotted) return;
        allTocNavs().forEach(snapshotTocOnce);
        snapshotted = true;
    }

    function restoreAllMemberContainers(){
        origContainerOrder.forEach(function(kids, container){
            kids.forEach(function(ch){ container.appendChild(ch); });
        });
    }

    function reorderMemberDetailsAlpha() {
        var sidebarNav = document.querySelector(".main-grid nav.toc");
        var mainRoot = document.querySelector(".main-grid main");
        if (!sidebarNav || !mainRoot) return;

        var containers = Array.from(
            mainRoot.querySelectorAll("ul.member-list")
        );

        containers.forEach(function(container) {
            var links = Array.from(sidebarNav.querySelectorAll("a[href^='#']")).filter(function(a) {
                var id = a.getAttribute("href").slice(1);
                if (!id) return false;
                var target = document.getElementById(decodeURI(id));
                return target && container.contains(target);
            });
            if (links.length < 2) return;

            var items = links.map(function(a) {
                var id = a.getAttribute("href").slice(1);
                var target = document.getElementById(decodeURI(id));
                if (!target) return null;
                var block = target.closest("section.detail, div.detail") || target;
                var li = block.closest("li");
                if (li.parentElement !== container) return null;
                return {
                    node: li,
                    text: (a.textContent || "").trim()
                };
            }).filter(Boolean);

            if (items.length < 2) return;

            if (!origContainerOrder.has(container)) {
                origContainerOrder.set(container, Array.from(container.children));
            }

            items.slice()
                .sort(function(x, y) {
                    return x.text.localeCompare(y.text, undefined, {
                        numeric: true,
                        sensitivity: "base"
                    });
                })
                .forEach(function(it) {
                    container.appendChild(it.node);
                });
        });
    }

    function updateToggleButtons(order){
        const next = (order === TOC_ALPHA) ? sortSourceLabel : sortLexicalLabel;
        document.querySelectorAll(".toc-sort-toggle").forEach(function(btn){
            btn.setAttribute("aria-label", next);
            btn.setAttribute("title", next);
            btn.setAttribute("aria-pressed", order === TOC_ALPHA);

            if (order === TOC_ALPHA) {
                btn.classList.add("toc-sort-is-active");
            } else {
                btn.classList.remove("toc-sort-is-active");
            }

            var img = btn.querySelector("img");
            if (img) img.alt = next;
        });
    }

    var tocOrder = TOC_SOURCE;
    updateToggleButtons(tocOrder);

    function applyAlpha(){
        snapshotAllOnce();
        reorderMemberDetailsAlpha();
        allTocNavs().forEach(sortTocAlpha);
        initSectionData(); handleScroll();
        updateToggleButtons(TOC_ALPHA);
        tocOrder = TOC_ALPHA;
    }

    function applySource(){
        snapshotAllOnce();
        restoreAllMemberContainers();
        allTocNavs().forEach(restoreToc);
        initSectionData(); handleScroll();
        updateToggleButtons(TOC_SOURCE);
        tocOrder = TOC_SOURCE;
    }

    document.querySelectorAll(".toc-sort-toggle").forEach(function(btn) {
      btn.addEventListener("click", function() {
          if (tocOrder === TOC_SOURCE) applyAlpha(); else applySource();
          if (typeof btn.blur === "function") btn.blur();
      });
    });

    // Resize handler
    new ResizeObserver((entries) => {
        if (expanded) {
            if (windowWidth !== window.innerWidth) {
                collapse();
            } else {
                expand();
            }
        }
        if (sections && document.body.offsetHeight !== bodyHeight) {
            initSectionData();
            prevHash = null;
            handleScroll();
        }
        setTopMargin();
    }).observe(document.body);
});
