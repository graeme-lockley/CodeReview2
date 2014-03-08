var RepoAuthor = Backbone.Model.extend({
});
var RepoAuthors = Backbone.Collection.extend({
    model: RepoAuthor,
    url: "/repos/1/authors"
});

var repoAuthors = new RepoAuthors();

$("#authorsBTN").click(function (item) {
    repoAuthors.fetch({
        success: function () {
            setHtml($("#repoAuthorAllocationList"), "repoAuthorAllocationItems", {repoAuthors: repoAuthors});
            $("#repoAuthorAllocation").modal("show");
        }
    });
});

function setHtml(domElement, templateName, templateState) {
    var theTemplate = $("#" + templateName).html();
    domElement.html(_.template(theTemplate, templateState));
}
