var loggedOnUser = undefined;

var RepoAuthor = Backbone.Model.extend({
    urlRoot: "/repoAuthors"
});

var RepoAuthors = Backbone.Collection.extend({
    model: RepoAuthor,
    url: "/repos/1/authors"
});

var RepoAuthorsList = Backbone.View.extend({
    initialize: function () {
        this.render();
    },
    render: function () {
        setHtml(this.$el, "repoAuthorAllocationItems", {repoAuthors: repoAuthors, loggedOnUser: loggedOnUser});
    },
    events: {
        "click input[type=checkbox]": "checkBoxChange"
    },
    checkBoxChange: function (event) {
        var repoAuthor = repoAuthors.get(event.currentTarget.id);

        if (repoAuthor.has("author")) {
            repoAuthor.unset("author");
        } else {
            repoAuthor.set("author", loggedOnUser);
        }
        repoAuthor.save()
    }
});

var repoAuthors = new RepoAuthors();
var repoAuthorsList = new RepoAuthorsList({el: $("#repoAuthorAllocationList")});

$("#authorsBTN").click(function (item) {
    repoAuthors.fetch({
        success: function () {
            repoAuthorsList.render();
            $("#repoAuthorAllocation").modal("show");
        }
    });
});

function setHtml(domElement, templateName, templateState) {
    var theTemplate = $("#" + templateName).html();
    domElement.html(_.template(theTemplate, templateState));
}

function fetchLoggedOnUser() {
    $.ajax({
        type: "GET",
        url: "/auth/user",
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        success: function (data, status, jqXHR) {
            loggedOnUser = data;
        },
        error: function (jqXHR, status) {
            console.error(jqXHR);
        }
    });
}

$(document).ready(function () {
    fetchLoggedOnUser();
});
