var loggedOnUser = undefined;

var Author = Backbone.Model.extend({
    urlRoot: "/authors"
});

var Authors = Backbone.Collection.extend({
    model: Author,
    url: "/authors"
});

var Revisions = Backbone.Model.extend({
    url: "/revisions"
});

var authors = new Authors();
var revisions = new Revisions();

function setHtml(domElement, templateName, templateState) {
    var theTemplate = $("#" + templateName).html();
    domElement.html(_.template(theTemplate, templateState));
}

function fetchLoggedOnUser(successContinuation) {
    $.ajax({
        type: "GET",
        url: "/auth/user",
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        success: function (data, status, jqXHR) {
            loggedOnUser = data;
            successContinuation();
        },
        error: function (jqXHR, status) {
            console.error(jqXHR);
        }
    });
}

$(document).ready(function () {
    fetchLoggedOnUser(function () {
        authors.fetch({
            success: function () {
            }
        });
    });
});
