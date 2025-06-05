/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

var moduleSearchIndex;
var packageSearchIndex;
var typeSearchIndex;
var memberSearchIndex;
var tagSearchIndex;

var oddRowColor = "odd-row-color";
var evenRowColor = "even-row-color";
var sortAsc = "sort-asc";
var sortDesc = "sort-desc";
var tableTab = "table-tab";
var activeTableTab = "active-table-tab";

const linkIcon = "Link icon";
const linkToSection = "Link to this section";

function loadScripts(doc, tag) {
    createElem(doc, tag, 'script-files/search.js');

    createElem(doc, tag, 'module-search-index.js');
    createElem(doc, tag, 'package-search-index.js');
    createElem(doc, tag, 'type-search-index.js');
    createElem(doc, tag, 'member-search-index.js');
    createElem(doc, tag, 'tag-search-index.js');
}

function createElem(doc, tag, path) {
    var script = doc.createElement(tag);
    var scriptElement = doc.getElementsByTagName(tag)[0];
    script.src = pathtoroot + path;
    scriptElement.parentNode.insertBefore(script, scriptElement);
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
function setTopMargin() {
    // Dynamically set scroll margin to accomodate for draft header
    var headerHeight = Math.ceil(document.querySelector("header").offsetHeight);
    document.querySelector(":root")
        .style.setProperty("--nav-height", headerHeight + "px");
}
document.addEventListener("readystatechange", (e) => {
    if (document.readyState === "interactive") {
        setTopMargin();
    }
    if (sessionStorage.getItem("sidebar") === "hidden") {
        const sidebar = document.querySelector(".main-grid nav.toc");
        if (sidebar) sidebar.classList.add("hide-sidebar");
    }
});
document.addEventListener("DOMContentLoaded", function(e) {
    setTopMargin();
    // Reset animation for type parameter target highlight
    document.querySelectorAll("a").forEach((link) => {
        link.addEventListener("click", (e) => {
            const href = e.currentTarget.getAttribute("href");
            if (href && href.startsWith("#") && href.indexOf("type-param-") > -1) {
                const target = document.getElementById(decodeURI(href.substring(1)));
                if (target) {
                    target.style.animation = "none";
                    void target.offsetHeight;
                    target.style.removeProperty("animation");
                }
            }
        })
    });
    // Make sure current element is visible in breadcrumb navigation on small displays
    const subnav = document.querySelector("ol.sub-nav-list");
    if (subnav && subnav.lastElementChild) {
        subnav.lastElementChild.scrollIntoView({ behavior: "instant", inline: "start", block: "nearest" });
    }
    // Clone TOC sidebar to header for mobile navigation
    const navbar = document.querySelector("div#navbar-top");
    const sidebar = document.querySelector(".main-grid nav.toc");
    const main = document.querySelector(".main-grid main");
    const mainnav = navbar.querySelector("ul.nav-list");
    const toggleButton = document.querySelector("button#navbar-toggle-button");
    const toc = sidebar ? sidebar.cloneNode(true) : null;
    if (toc) {
        navbar.appendChild(toc);
    }
    document.querySelectorAll("input.filter-input").forEach(function(input) {
        input.removeAttribute("disabled");
        input.setAttribute("autocapitalize", "off");
        input.value = "";
        input.addEventListener("input", function(e) {
            const pattern = input.value ? input.value.trim()
                .replace(/[\[\]{}()*+?.\\^$|]/g, '\\$&')
                .replace(/\s+/g, ".*") : "";
            input.nextElementSibling.style.display = pattern ? "inline" : "none";
            const filter = new RegExp(pattern, "i");
            input.parentNode.parentNode.querySelectorAll("ol.toc-list li").forEach((li) => {
                if (filter.test(li.innerText)) {
                    li.removeAttribute("style");
                } else {
                    li.style.display = "none";
                }
            });
            if (expanded) {
                expand();
            }
        });
    });
    document.querySelectorAll("input.reset-filter").forEach((button) => {
        button.removeAttribute("disabled");
        button.addEventListener("click", (e) => {
            const input = button.previousElementSibling;
            input.value = "";
            input.dispatchEvent(new InputEvent("input"));
            input.focus();
            if (expanded) {
                expand();
            } else {
                prevHash = null;
                handleScroll();
            }
        })
    });
    var expanded = false;
    var windowWidth;
    var bodyHeight;
    function collapse(e) {
        if (expanded) {
            mainnav.removeAttribute("style");
            if (toc) {
                toc.removeAttribute("style");
            }
            toggleButton.classList.remove("expanded")
            toggleButton.setAttribute("aria-expanded", "false");
            expanded = false;
        }
    }
    function expand() {
        expanded = true;
        mainnav.style.display = "block";
        mainnav.style.removeProperty("height");
        var maxHeight = window.innerHeight - subnav.offsetTop + 4;
        var expandedHeight = Math.min(maxHeight, mainnav.scrollHeight + 10);
        if (toc) {
            toc.style.display = "flex";
            expandedHeight = Math.min(maxHeight,
                Math.max(expandedHeight, toc.querySelector("div.toc-header").offsetHeight
                                       + toc.querySelector("ol.toc-list").scrollHeight + 10));
            toc.style.height = expandedHeight + "px";
        }
        mainnav.style.height = expandedHeight + "px";
        toggleButton.classList.add("expanded");
        toggleButton.setAttribute("aria-expanded", "true");
        windowWidth = window.innerWidth;
    }
    toggleButton.addEventListener("click", (e) => {
        if (expanded) {
            collapse();
        } else {
            expand();
        }
    });
    if (toc) {
        toc.querySelectorAll("a").forEach((link) => {
            link.addEventListener("click", collapse);
        });
    }
    document.addEventListener('keydown', (e) => {
        if (e.key === "Escape") collapse();
    });
    document.querySelector("main").addEventListener("click", collapse);
    const searchInput = document.getElementById("search-input");
    if (searchInput) searchInput.addEventListener("focus", collapse);
    document.querySelectorAll("h1, h2, h3, h4, h5, h6")
        .forEach((hdr, idx) => {
            // Create anchor links for headers with an associated id attribute
            var id = hdr.parentElement.getAttribute("id") || hdr.getAttribute("id")
                || (hdr.querySelector("a") && hdr.querySelector("a").getAttribute("id"));
            if (id) {
                var template = document.createElement('template');
                template.innerHTML =" <a href='#" + encodeURI(id) + "' class='anchor-link' aria-label='" + linkToSection
                    + "'><img src='" + pathtoroot + "resource-files/link.svg' alt='" + linkIcon +"' tabindex='0'"
                    + " width='16' height='16'></a>";
                hdr.append(...template.content.childNodes);
            }
        });
    var sections;
    var scrollTimeout;
    var scrollTimeoutNeeded;
    var prevHash;
    function initSectionData() {
        bodyHeight = document.body.offsetHeight;
        sections = [{ id: "", top: 0 }].concat(Array.from(main.querySelectorAll("section[id], h2[id], h2 a[id], div[id]"))
            .filter((e) => {
                return sidebar.querySelector("a[href=\"#" + encodeURI(e.getAttribute("id")) + "\"]") !== null
            }).map((e) => {
                return {
                    id: e.getAttribute("id"),
                    top: e.offsetTop
                };
            }));
    }
    function setScrollTimeout() {
        clearTimeout(scrollTimeout);
        scrollTimeoutNeeded = false;
        scrollTimeout = setTimeout(() => {
            scrollTimeout = null;
            handleScroll();
        }, 100);
    }
    function handleScroll() {
        if (!sidebar || !sidebar.offsetParent || sidebar.classList.contains("hide-sidebar")) {
            return;
        }
        if (scrollTimeout || scrollTimeoutNeeded) {
            setScrollTimeout();
            return;
        }
        var scrollTop = document.documentElement.scrollTop;
        var scrollHeight = document.documentElement.scrollHeight;
        var currHash = null;
        if (scrollHeight - scrollTop < window.innerHeight + 10) {
            // Select last item if at bottom of the page
            currHash = "#" + encodeURI(sections.at(-1).id);
        } else {
            for (var i = 0; i < sections.length; i++) {
                var top = sections[i].top;
                var bottom = sections[i + 1] ? sections[i + 1].top : scrollHeight;
                if (top + ((bottom - top) / 2) > scrollTop || bottom > scrollTop + (window.innerHeight / 3)) {
                    currHash = "#" + encodeURI(sections[i].id);
                    break;
                }
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
    if (sidebar) {
        initSectionData();
        document.querySelectorAll("a[href^='#']").forEach((link) => {
            link.addEventListener("click", (e) => {
                scrollTimeoutNeeded = true;
                setSelected(link.getAttribute("href"));
            })
        });
        sidebar.querySelector("button.hide-sidebar").addEventListener("click", () => {
            sidebar.classList.add("hide-sidebar");
            sessionStorage.setItem("sidebar", "hidden");
        });
        sidebar.querySelector("button.show-sidebar").addEventListener("click", () => {
            sidebar.classList.remove("hide-sidebar");
            sessionStorage.removeItem("sidebar");
            initSectionData();
            handleScroll();
        });
        window.addEventListener("hashchange", (e) => {
            scrollTimeoutNeeded = true;
        });
        if (document.location.hash) {
            scrollTimeoutNeeded = true;
            setSelected(document.location.hash);
        } else {
            handleScroll();
        }
        window.addEventListener("scroll", handleScroll);
        window.addEventListener("scrollend", () => {
            if (scrollTimeout) {
                clearTimeout(scrollTimeout);
                scrollTimeout = null;
                handleScroll();
            }
        })
    }
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
