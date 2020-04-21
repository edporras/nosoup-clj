//
function load()
{
    // from somewhere via google... sigh
    var search = document.getElementById('search');
    if (search) {
        search.style.display = 'none';
    }
    else {
        if (document.layers) {
            document.search.display = 'none';
        }
        else {
            document.all.search.style.display = 'none';
        }
    }
}

//
//
function selChange()
{
    var menu = document["catlist"]["cat"];
    var cat = menu.options[ menu.selectedIndex ].value;
    var url = "/";

    // strip the "all"
    if ("all".localeCompare(cat, 'en', {sensitivity: 'base'})) {
        url += cat + "/";
    }

    window.location.href = url;
    return true;
}
